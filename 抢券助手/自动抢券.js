"ui";
/**
 * 京东外卖「抢券助手」—— Auto.js / AutoX 脚本（安卓手机端运行）
 * ------------------------------------------------------------------
 * 作用：每天到饭点（默认 10:00、17:00）自动打开微信 -> 进入「京东外卖」小程序
 *       -> 在"抢券"按钮位置高速连点，尽量抢到券。
 *
 * ⚠️ 使用前须知
 *  1. 本脚本依赖「无障碍服务」模拟点击，属于对京东/微信的自动化操作，
 *     违反平台用户协议，可能被限流或封号。请自行评估，仅限本人账号、低频使用。
 *  2. "抢券"按钮坐标每台手机都不同，必须先用本脚本的【采集坐标】按钮在你手机上录一次。
 *  3. 手机需保持亮屏、已解锁（或免密），否则到点无法点击。
 *  4. 金额/库存相关的最终结果由平台决定，脚本只负责"点得快"。
 * ------------------------------------------------------------------
 */

// ============ 1. 读取/保存配置 ============
var store = storages.create("jd_coupon_helper");

var CONFIG = {
    times:     store.get("times", "10:00:00,17:00:00"), // 抢券时间点，逗号分隔
    leadSec:   store.get("leadSec", 20),   // 提前多少秒准备（唤醒+打开小程序）
    spamSec:   store.get("spamSec", 8),    // 到点后连点按钮的秒数
    miniProgram: store.get("miniProgram", "京东外卖"), // 小程序名称
    grabX:     store.get("grabX", 540),    // 抢券按钮 X 坐标
    grabY:     store.get("grabY", 1600)    // 抢券按钮 Y 坐标
};

function saveConfig() {
    store.put("times", CONFIG.times);
    store.put("leadSec", CONFIG.leadSec);
    store.put("spamSec", CONFIG.spamSec);
    store.put("miniProgram", CONFIG.miniProgram);
    store.put("grabX", CONFIG.grabX);
    store.put("grabY", CONFIG.grabY);
}

// ============ 2. 抢券核心逻辑 ============

// 解析时间点，返回距离下一个抢券时刻的毫秒数
function msUntilNext() {
    var list = CONFIG.times.split(",");
    var now = new Date();
    var best = Infinity;
    for (var i = 0; i < list.length; i++) {
        var parts = list[i].trim().split(":");
        if (parts.length < 2) continue;
        var d = new Date(now.getFullYear(), now.getMonth(), now.getDate(),
            parseInt(parts[0]), parseInt(parts[1]), parseInt(parts[2] || "0"));
        if (d.getTime() <= now.getTime()) d.setDate(d.getDate() + 1);
        best = Math.min(best, d.getTime() - now.getTime());
    }
    return best;
}

// 唤醒屏幕
function wakeScreen() {
    try { device.wakeUp(); } catch (e) { }
    try { device.keepScreenOn(3 * 60 * 60 * 1000); } catch (e) { }
    sleep(400);
}

// 打开微信并进入京东外卖小程序
function openMiniProgram() {
    app.launchPackage("com.tencent.mm");   // 微信包名
    sleep(2500);

    // 微信首页下拉 -> 呼出「最近使用/我的小程序」面板
    // （不同微信版本可能略有差异，如失效可改用：在微信搜索框搜"京东外卖"）
    swipe(540, 300, 540, 1400, 400);
    sleep(1200);

    // 在小程序列表里找名字包含「京东外卖」的入口并点进去
    var entry = textContains(CONFIG.miniProgram).findOnce();
    if (!entry) {
        // 兜底：搜索方式
        click(device.width / 2, 120); // 点顶部搜索
        sleep(800);
        setText(CONFIG.miniProgram);
        sleep(800);
        var s = textContains(CONFIG.miniProgram).findOnce();
        if (s) s.click();
    } else {
        entry.click();
    }
    sleep(3500); // 等小程序加载出来
}

// 高速连点「抢券」按钮
function spamTap(seconds) {
    var end = Date.now() + seconds * 1000;
    while (Date.now() < end) {
        click(CONFIG.grabX, CONFIG.grabY);
        // 极短间隔，避免sleep拖慢；如需更快可减小
        sleep(15);
    }
}

// 一次完整的抢券流程
function doGrab() {
    toastLog("准备抢券…");
    wakeScreen();
    openMiniProgram();
    var wait = CONFIG.leadSec; // openMiniProgram 已耗时，剩余再等到点
    toastLog("已就位，等待开抢…");
    // 精确定位到时间点（简单轮询，误差 <50ms 级）
    while (true) {
        var d = new Date();
        var secsToday = d.getHours() * 3600 + d.getMinutes() * 60 + d.getSeconds();
        var hit = false;
        var list = CONFIG.times.split(",");
        for (var i = 0; i < list.length; i++) {
            var p = list[i].trim().split(":");
            var target = parseInt(p[0]) * 3600 + parseInt(p[1]) * 60 + parseInt(p[2] || "0");
            if (Math.abs(secsToday - target) <= 1) { hit = true; break; }
        }
        if (hit) break;
        sleep(30);
    }
    toastLog("开始抢！");
    spamTap(CONFIG.spamSec);
    toastLog("本轮结束。");
}

// ============ 3. 悬浮采集坐标 ============
function pickCoord(label, cb) {
    var w = floaty.rawWindow(
        <frame id="cap" w="*" h="*" bg="#44000000">
            <text id="tip" text="点击「抢券」按钮的位置\n（点一下即可，采集后自动关闭）"
                  textColor="#ffffff" textSize="18sp" gravity="center"/>
        </frame>
    );
    w.setSize(-1, -1);
    w.cap.setTouchable(true);
    w.cap.setOnTouchListener(function (view, event) {
        if (event.getAction() === event.ACTION_DOWN) {
            var x = event.getRawX(), y = event.getRawY();
            toastLog(label + "：x=" + x + " y=" + y);
            ui.run(function () { w.close(); });
            cb(x, y);
            return true;
        }
        return true;
    });
}

// ============ 4. 脚本主流程（带上运行开关）============
var RUNNING = false;
threads.start(function () {
    // 等待无障碍服务
    auto.waitFor();
    toastLog("抢券助手已就绪，等待到达抢券时间点…");
    while (true) {
        if (!RUNNING) { sleep(1000); continue; }
        var ms = msUntilNext();
        var targetTime = new Date(Date.now() + ms).toTimeString().slice(0, 8);
        // 提前 leadSec 秒开始准备
        var prepareAt = ms - CONFIG.leadSec * 1000;
        if (prepareAt > 0) {
            toastLog("下一次开抢：" + targetTime + "（约 " + Math.round(ms / 60000) + " 分钟后）");
            sleep(prepareAt);
        }
        if (RUNNING) doGrab();
        sleep(2000);
    }
});

// ============ 5. 界面 ============
ui.layout(
    <vertical padding="16">
        <text text="京东外卖 抢券助手" textSize="22sp" textColor="#333333" gravity="center"/>
        <text text="点开始后在后台等点自动抢，手机需保持亮屏解锁" textSize="12sp" textColor="#999999" marginTop="4"/>

        <text text="抢券时间点（逗号分隔，24小时制）" marginTop="16"/>
        <input id="times" text="{{CONFIG.times}}"/>

        <text text="小程序名称" marginTop="8"/>
        <input id="miniProgram" text="{{CONFIG.miniProgram}}"/>

        <text text="提前准备秒数 / 连点秒数" marginTop="8"/>
        <linear>
            <input id="leadSec" w="0" layout_weight="1" inputType="number" text="{{CONFIG.leadSec}}"/>
            <input id="spamSec" w="0" layout_weight="1" inputType="number" text="{{CONFIG.spamSec}}"/>
        </linear>

        <text text="「抢券」按钮坐标" marginTop="8"/>
        <linear>
            <input id="grabX" w="0" layout_weight="1" inputType="number" text="{{CONFIG.grabX}}"/>
            <input id="grabY" w="0" layout_weight="1" inputType="number" text="{{CONFIG.grabY}}"/>
        </linear>
        <button id="pick" text="采集按钮坐标（点一下屏幕上的抢券按钮）"/>

        <button id="save" text="保存配置" style="Widget.AppCompat.Button.Colored"/>
        <button id="toggle" text="▶ 开始运行" bg="#4CAF50" textColor="#ffffff"/>
        <button id="test" text="立即测试一次抢券流程"/>
    </vertical>
);

ui.times.setText(CONFIG.times);
ui.miniProgram.setText(CONFIG.miniProgram);
ui.leadSec.setText(String(CONFIG.leadSec));
ui.spamSec.setText(String(CONFIG.spamSec));
ui.grabX.setText(String(CONFIG.grabX));
ui.grabY.setText(String(CONFIG.grabY));

function readUI() {
    CONFIG.times = ui.times.getText().toString();
    CONFIG.miniProgram = ui.miniProgram.getText().toString();
    CONFIG.leadSec = parseInt(ui.leadSec.getText().toString()) || 20;
    CONFIG.spamSec = parseInt(ui.spamSec.getText().toString()) || 8;
    CONFIG.grabX = parseInt(ui.grabX.getText().toString()) || 0;
    CONFIG.grabY = parseInt(ui.grabY.getText().toString()) || 0;
}

ui.save.click(function () {
    readUI(); saveConfig();
    toast("已保存："+CONFIG.times);
});

ui.pick.click(function () {
    readUI();
    threads.start(function () {
        pickCoord("抢券按钮", function (x, y) {
            CONFIG.grabX = x; CONFIG.grabY = y; saveConfig();
            ui.run(function () {
                ui.grabX.setText(String(x));
                ui.grabY.setText(String(y));
            });
        });
    });
});

ui.test.click(function () {
    readUI();
    threads.start(function () {
        wakeScreen();
        openMiniProgram();
        spamTap(3); // 测试只点3秒
        toastLog("测试结束");
    });
});

ui.toggle.click(function () {
    readUI(); saveConfig();
    RUNNING = !RUNNING;
    ui.run(function () {
        ui.toggle.setText(RUNNING ? "⏸ 运行中（点击暂停）" : "▶ 开始运行");
    });
});

ui.emitter.on("pause", function () {
    // 脚本切后台继续跑
});
