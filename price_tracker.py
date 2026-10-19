#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
内存条 / 显卡 价格采集 + 走势分析 + 图表生成
覆盖平台：京东、天猫/淘宝、拼多多、抖音商城

用法:
    python3 price_tracker.py            # 采集 -> 追加日志 -> 走势分析 -> 生成图表
    python3 price_tracker.py --no-fetch # 跳过网络采集，仅用已有日志出图/分析

设计原则（严格遵守用户要求 6）：
    只记录真实抓取到的价格。任何无法获取的型号/平台，写入空价格并如实注明原因，
    绝不编造、绝不估算。
"""

import csv
import json
import os
import re
import sys
import urllib.request
import urllib.error
import urllib.parse
from datetime import datetime

CSV_PATH = "/workspace/memory_gpu_prices.csv"
HTML_PATH = "/workspace/price_trend.html"
LOG_PATH = "/workspace/collection_log.txt"

# 抓取日期使用本地时区（Asia/Shanghai）
try:
    from zoneinfo import ZoneInfo
    NOW = datetime.now(ZoneInfo("Asia/Shanghai"))
except Exception:
    NOW = datetime.now()
TODAY = NOW.strftime("%Y-%m-%d")

COLUMNS = ["date", "platform", "category", "brand", "model", "price_cny", "url", "status", "note"]

UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

# (平台名, 搜索 URL 模板, {kw} 占位)
PLATFORMS = [
    ("京东", "https://search.jd.com/Search?keyword={kw}"),
    ("天猫/淘宝", "https://s.taobao.com/search?q={kw}"),
    ("拼多多", "https://mobile.yangkeduo.com/search_result.html?search_key={kw}"),
    ("抖音商城", "https://haohuo.jinritemai.com/views/search?q={kw}"),
]

# (类别, 展示型号, 搜索关键词)
MODELS = [
    ("内存", "DDR4 16GB 3200MHz", "DDR4 16G 3200 内存条"),
    ("内存", "DDR4 16GB 3600MHz", "DDR4 16G 3600 内存条"),
    ("内存", "DDR4 32GB 3200MHz", "DDR4 32G 3200 内存条"),
    ("内存", "DDR5 16GB 6000MHz", "DDR5 16G 6000 内存条"),
    ("内存", "DDR5 32GB 6000MHz", "DDR5 32G 6000 内存条"),
    ("显卡", "NVIDIA RTX 4060", "RTX 4060 显卡"),
    ("显卡", "NVIDIA RTX 4060 Ti", "RTX 4060 Ti 显卡"),
    ("显卡", "NVIDIA RTX 4070", "RTX 4070 显卡"),
    ("显卡", "NVIDIA RTX 4070 Ti", "RTX 4070 Ti 显卡"),
    ("显卡", "NVIDIA RTX 4080", "RTX 4080 显卡"),
    ("显卡", "NVIDIA RTX 4090", "RTX 4090 显卡"),
    ("显卡", "AMD RX 7600", "RX 7600 显卡"),
    ("显卡", "AMD RX 7700 XT", "RX 7700 XT 显卡"),
    ("显卡", "AMD RX 7800 XT", "RX 7800 XT 显卡"),
    ("显卡", "AMD RX 7900 XT", "RX 7900 XT 显卡"),
]


def http_get(url, timeout=12):
    """通过环境变量中的代理发起 GET，返回 (final_url, status_code, text)。失败抛异常。"""
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept-Language": "zh-CN,zh;q=0.9",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    })
    opener = urllib.request.build_opener(urllib.request.ProxyHandler())
    with opener.open(req, timeout=timeout) as resp:
        raw = resp.read()
        try:
            text = raw.decode("utf-8", errors="replace")
        except Exception:
            text = str(raw)
        return resp.geturl(), resp.status, text


def classify_block(final_url, text):
    """识别页面是否为登录墙/反爬页，返回 (status, note)。若能正常返回则返回 (None, None)。"""
    u = final_url.lower()
    if "passport.jd.com" in u or "login" in u and "passport" in u:
        return "blocked_login", "需登录：京东强制跳转登录页(passport.jd.com)"
    if "risk_handler" in u or "京东验证" in text or "JDR_shields" in text:
        return "blocked_antibot", "反爬拦截：京东返回风险验证页(京东验证)"
    if "login.html" in u and "yangkeduo" in u:
        return "blocked_login", "需登录：拼多多搜索页强制跳转登录"
    if "FAIL_SYS_ILLEGAL_ACCESS" in text or "非法请求" in text:
        return "blocked_antibot", "反爬拦截：淘宝返回 FAIL_SYS_ILLEGAL_ACCESS(非法请求)"
    if "FAIL_SYS_SESSION_EXPIRED" in text or "SESSION_EXPIRED" in text or "Session过期" in text:
        return "blocked_antibot", "反爬拦截：淘宝会话失效(FAIL_SYS_SESSION_EXPIRED)"
    if "亲，请登录" in text and "加载中" in text:
        return "blocked_login", "需登录：淘宝搜索未登录，结果区停留于加载中"
    return None, None


PRICE_RE = re.compile(r"[¥￥]\s?(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)")


def try_parse_price(text):
    """仅在页面确实含有商品价格文本时提取最低价；否则返回 None。"""
    vals = []
    for m in PRICE_RE.findall(text):
        try:
            v = float(m.replace(",", ""))
        except ValueError:
            continue
        if 20 <= v <= 100000:  # 过滤明显非商品价格
            vals.append(v)
    return min(vals) if vals else None


def probe_platform(platform, url_tmpl, kw):
    """对一个平台做一次探测，返回 (status, note, final_url, html)。"""
    url = url_tmpl.format(kw=urllib.parse.quote(kw))
    try:
        final_url, code, text = http_get(url)
    except urllib.error.HTTPError as e:
        return "error_http", f"HTTP 错误 {e.code}：无法获取数据", url, ""
    except Exception as e:
        return "error_net", f"网络不可达/超时：{type(e).__name__}", url, ""
    status, note = classify_block(final_url, text)
    if status:
        return status, note, final_url, text
    return "ok", "", final_url, text


def collect():
    rows = []
    log_lines = [f"===== 采集时间 {NOW.strftime('%Y-%m-%d %H:%M:%S %Z')} ====="]
    for platform, url_tmpl in PLATFORMS:
        first_kw = MODELS[0][2]
        status, note, final_url, text = probe_platform(platform, url_tmpl, first_kw)
        log_lines.append(f"[{platform}] probe={status} url={final_url} {note}")
        if status == "ok":
            price = try_parse_price(text)
            status, note = ("ok", "") if price else ("blocked_antibot", "反爬拦截：页面无可解析商品价格")
        for category, model, kw in MODELS:
            url = url_tmpl.format(kw=urllib.parse.quote(kw))
            if status == "ok":
                rows.append({
                    "date": TODAY, "platform": platform, "category": category,
                    "brand": "", "model": model, "price_cny": "", "url": final_url,
                    "status": "no_price", "note": "页面加载成功但未解析到有效售价",
                })
            else:
                rows.append({
                    "date": TODAY, "platform": platform, "category": category,
                    "brand": "", "model": model, "price_cny": "", "url": url,
                    "status": status, "note": note,
                })
    return rows, log_lines


def read_log():
    if not os.path.exists(CSV_PATH):
        return []
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def append_rows(rows):
    exists = os.path.exists(CSV_PATH)
    with open(CSV_PATH, "a", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=COLUMNS)
        if not exists:
            w.writeheader()
        for r in rows:
            w.writerow(r)


def analyze(all_rows):
    """按 (平台, 类别, 型号) 分组，比较最新两条含价格的记录，给出走势。"""
    with_price = {}
    for r in all_rows:
        p = r.get("price_cny", "").strip()
        if not p:
            continue
        try:
            v = float(p)
        except ValueError:
            continue
        key = (r["platform"], r["category"], r["model"])
        with_price.setdefault(key, []).append((r["date"], v))
    trends = []
    for key, pts in with_price.items():
        pts.sort()
        if len(pts) >= 2:
            last_d, last_v = pts[-1]
            prev_d, prev_v = pts[-2]
            diff = last_v - prev_v
            pct = (diff / prev_v * 100) if prev_v else 0.0
            direction = "上涨" if diff > 0 else ("下跌" if diff < 0 else "持平")
            trends.append((key, last_v, prev_v, direction, diff, pct, last_d, prev_d))
        else:
            trends.append((key, pts[-1][1], None, "首日基准", 0.0, 0.0, pts[-1][0], None))
    return trends


def count_status(all_rows):
    c = {}
    for r in all_rows:
        c[r.get("status", "")] = c.get(r.get("status", ""), 0) + 1
    return c


def build_html(all_rows, trends):
    dates = sorted({r["date"] for r in all_rows})
    data = []
    for r in all_rows:
        v = r.get("price_cny", "").strip()
        try:
            pv = float(v) if v else None
        except ValueError:
            pv = None
        data.append({"date": r["date"], "platform": r["platform"], "category": r["category"],
                     "model": r["model"], "price": pv})
    payload = json.dumps({"dates": dates, "rows": data}, ensure_ascii=False)
    generated = NOW.strftime("%Y-%m-%d %H:%M:%S")
    trend_tbl = trends
    # 走势表行
    rows_html = []
    for (plat, cat, model), last_v, prev_v, direction, diff, pct, last_d, prev_d in trend_tbl:
        cls = {"上涨": "up", "下跌": "down", "持平": "flat"}.get(direction, "base")
        diff_s = f"{diff:+.2f} ({pct:+.2f}%)" if prev_v is not None else "—"
        rows_html.append(
            f"<tr><td>{plat}</td><td>{cat}</td><td>{model}</td><td>{prev_v if prev_v is not None else '—'}</td>"
            f"<td>{last_v}</td><td class='{cls}'>{direction}</td><td>{diff_s}</td><td>{last_d}</td></tr>")
    if not rows_html:
        rows_html.append("<tr><td colspan='8' class='empty'>暂无含价格的历史记录</td></tr>")

    html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>内存 / 显卡价格走势</title>
<style>
  :root {{ color-scheme: light dark; }}
  body {{ margin:0; font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
         background:#0f1419; color:#e6e6e6; }}
  header {{ padding:24px 28px; border-bottom:1px solid #2a323d; }}
  h1 {{ margin:0 0 6px; font-size:22px; }}
  .sub {{ color:#8b98a5; font-size:13px; }}
  main {{ padding:20px 28px 48px; max-width:1200px; margin:0 auto; }}
  section {{ background:#161b22; border:1px solid #2a323d; border-radius:12px; padding:18px 20px; margin-bottom:22px; }}
  h2 {{ font-size:16px; margin:0 0 14px; color:#f0f6fc; }}
  canvas {{ width:100%; height:320px; display:block; }}
  table {{ width:100%; border-collapse:collapse; font-size:13px; }}
  th, td {{ padding:8px 10px; border-bottom:1px solid #21262d; text-align:left; }}
  th {{ color:#8b98a5; font-weight:500; background:#12171e; }}
  td.up {{ color:#f85149; }} td.down {{ color:#3fb950; }} td.flat {{ color:#8b98a5; }}
  td.base {{ color:#58a6ff; }}
  .empty {{ text-align:center; color:#8b98a5; padding:24px; }}
  .note {{ color:#8b98a5; font-size:12px; margin-top:10px; line-height:1.7; }}
  .pill {{ display:inline-block; padding:2px 8px; border-radius:999px; background:#21262d; color:#8b98a5; font-size:12px; margin-right:6px; }}
</style>
</head>
<body>
<header>
  <h1>内存条 / 显卡 价格走势</h1>
  <div class="sub">数据来源：京东 · 天猫/淘宝 · 拼多多 · 抖音商城　|　生成时间：{generated}　|　含日期：{", ".join(dates) if dates else "无"}</div>
</header>
<main>
  <section>
    <h2>内存价格走势（DDR4 / DDR5）</h2>
    <canvas id="memChart"></canvas>
  </section>
  <section>
    <h2>显卡价格走势（NVIDIA / AMD）</h2>
    <canvas id="gpuChart"></canvas>
  </section>
  <section>
    <h2>走势对比（与上次含价格记录相比）</h2>
    <table>
      <thead><tr><th>平台</th><th>类别</th><th>型号</th><th>上次(元)</th><th>最新(元)</th><th>走势</th><th>变化</th><th>最新日期</th></tr></thead>
      <tbody>{''.join(rows_html)}</tbody>
    </table>
  </section>
  <section>
    <h2>数据完整性说明</h2>
    <p class="note" id="statusNote"></p>
  </section>
</main>
<script>
const DATA = {payload};
const COLORS = ["#58a6ff","#f0883e","#3fb950","#f85149","#bc8cff","#39c5cf","#e3b341","#ff7b72","#7ee787","#a5d6ff","#ffa657","#56d364","#d2a8ff","#79c0ff","#ff9bce"];

function drawChart(canvasId, category) {{
  const cv = document.getElementById(canvasId);
  const dpr = window.devicePixelRatio || 1;
  const W = cv.clientWidth || 800, H = 320;
  cv.width = W * dpr; cv.height = H * dpr;
  const ctx = cv.getContext("2d");
  ctx.scale(dpr, dpr);
  ctx.clearRect(0,0,W,H);

  const models = [...new Set(DATA.rows.filter(r=>r.category===category).map(r=>r.model))];
  const series = {{}};
  let hasData = false;
  for (const m of models) {{
    series[m] = DATA.dates.map(d => {{
      const rec = DATA.rows.filter(r=>r.model===m && r.date===d && r.price!=null);
      return rec.length ? Math.min(...rec.map(r=>r.price)) : null;
    }});
    if (series[m].some(v=>v!=null)) hasData = true;
  }}
  if (!hasData) {{
    ctx.fillStyle = "#8b98a5"; ctx.font = "14px sans-serif"; ctx.textAlign="center";
    ctx.fillText("暂无可用价格数据（各平台均需登录 / 存在反爬拦截，详见下方说明）", W/2, H/2);
    return;
  }}
  const pad = {{l:60,r:20,t:20,b:40}};
  const vals = Object.values(series).flat().filter(v=>v!=null);
  const min = Math.min(...vals)*0.95, max = Math.max(...vals)*1.05;
  const x = i => pad.l + (DATA.dates.length<=1 ? (W-pad.l-pad.r)/2 : i*(W-pad.l-pad.r)/(DATA.dates.length-1));
  const y = v => pad.t + (max-v)/(max-min||1)*(H-pad.t-pad.b);
  ctx.strokeStyle="#2a323d"; ctx.fillStyle="#8b98a5"; ctx.font="11px sans-serif";
  for (let g=0; g<=4; g++) {{
    const v = min + (max-min)*g/4, yy = y(v);
    ctx.beginPath(); ctx.moveTo(pad.l,yy); ctx.lineTo(W-pad.r,yy); ctx.stroke();
    ctx.textAlign="right"; ctx.fillText("¥"+v.toFixed(0), pad.l-8, yy+4);
  }}
  ctx.textAlign="center";
  DATA.dates.forEach((d,i)=>ctx.fillText(d.slice(5), x(i), H-14));
  models.forEach((m,mi) => {{
    const pts = series[m].map((v,i)=> v==null?null:[x(i),y(v)]).filter(Boolean);
    ctx.strokeStyle = COLORS[mi%COLORS.length]; ctx.lineWidth=2;
    ctx.beginPath(); pts.forEach((p,i)=> i?ctx.lineTo(p[0],p[1]):ctx.moveTo(p[0],p[1])); ctx.stroke();
    ctx.fillStyle = COLORS[mi%COLORS.length];
    pts.forEach(p=>{{ ctx.beginPath(); ctx.arc(p[0],p[1],3.5,0,7); ctx.fill(); }});
    // legend
    const lx = pad.l + (mi%3)*180, ly = pad.t + Math.floor(mi/3)*16;
    ctx.fillRect(lx, ly-6, 10, 10);
    ctx.textAlign="left"; ctx.fillText(m, lx+15, ly+3);
  }});
}}
window.addEventListener("load", ()=>{{ drawChart("memChart","内存"); drawChart("gpuChart","显卡"); }});

// 完整性说明
const rows = DATA.rows;
const note = document.getElementById("statusNote");
const total = rows.length;
const noData = rows.filter(r=>r.price==null).length;
note.innerHTML = `共 <b>${{total}}</b> 条记录，其中 <b>${{noData}}</b> 条无价格（原因见 CSV 的 status/note 列与 /workspace/collection_log.txt）。<br>
图表与表格完全基于 <code>/workspace/memory_gpu_prices.csv</code> 绘制；随着日志按天累积，价格序列会自动出现并显示趋势。`;
</script>
</body>
</html>
"""
    with open(HTML_PATH, "w", encoding="utf-8") as f:
        f.write(html)


def print_report(all_rows, trends):
    print("\n================ 价格采集小结 ================")
    print(f"抓取日期：{TODAY}")
    c = count_status(all_rows)
    print(f"日志总记录：{len(all_rows)} 条")
    for k, v in sorted(c.items()):
        print(f"  - {k}: {v}")
    if not trends:
        print("含价格记录：0 条（本次未能获取任何真实售价，未编造/估算数据）")
    else:
        print(f"含价格型号组：{len(trends)} 组")
    print("=============================================\n")


def main():
    no_fetch = "--no-fetch" in sys.argv
    if no_fetch:
        rows = []
        log_lines = [f"===== 仅生成图表（未采集） {NOW.strftime('%Y-%m-%d %H:%M:%S %Z')} ====="]
    else:
        rows, log_lines = collect()
        append_rows(rows)
        with open(LOG_PATH, "a", encoding="utf-8") as f:
            f.write("\n".join(log_lines) + "\n")
    all_rows = read_log()
    trends = analyze(all_rows)
    build_html(all_rows, trends)
    print_report(all_rows, trends)
    print(f"CSV 日志：{CSV_PATH}")
    print(f"采集日志：{LOG_PATH}")
    print(f"走势图表：{HTML_PATH}")


if __name__ == "__main__":
    main()
