# Agent App 🤖💬

多 API 角色扮演聊天客户端 — 支持 OpenAI / Claude / DeepSeek 等多种后端，SillyTavern 风格 UI。

> ⚡ 本 app 使用 **Jetpack Compose + Material 3** 构建，在 **Termux** 环境下开发编译。

---

## ✨ 特性

- 🔌 **多 API 兼容** — OpenAI 格式（DeepSeek / OpenAI / NVIDIA / Gemini / Groq / 自定义）+ Anthropic Claude
- 💬 **SillyTavern 风格对话** — 角色左侧头像气泡，用户右侧消息，swipe 版本切换
- 🌿 **分支对话** — 长按消息创建分支，轻松探索不同剧情线
- 📝 **内联编辑** — 编辑消息内容，支持仅保存或保存后重新生成
- 📖 **世界书 (World Book)** — 三级结构（角色→世界书→条目），空关键词=始终触发
- 📋 **预设系统** — 保存 Temperature / TopP / MaxTokens 等参数组合，快速切换
- 🔄 **流式输出** — 实时流式显示 AI 回复，底部 LED 状态栏
- 🎨 **深蓝灰/米白双色主题** — 暗橙点缀，护眼舒适
- 🤖 **深度提示注入** — 角色→深度指令→世界书 AFTER_SYSTEM 层级
- 🔒 **本地存储** — Room 数据库，所有数据仅保存在本地

---

## 📥 下载 APK

> 🚧 最新 APK 通过 GitHub Actions 自动构建，点击下方按钮下载：

[![Download APK](https://img.shields.io/github/actions/workflow/status/ccy-nb/ccy/build.yml?label=Build%20APK&logo=github)](https://github.com/ccy-nb/ccy/actions/workflows/build.yml)

**手动下载：**
1. 打开 [Actions 页面](https://github.com/ccy-nb/ccy/actions/workflows/build.yml)
2. 点击最新的成功构建
3. 在 Artifacts 区下载 `app-debug.apk`

### 历史版本
| 版本 | 下载 |
|------|------|
| v2.9.3 | 暂无 Release — 使用 Actions 构建 |

---

## 🛠 自行构建 (Termux)

```bash
# 1. 克隆仓库
git clone https://github.com/ccy-nb/ccy.git ~/agent\ app

# 2. 进入目录
cd ~/agent\ app

# 3. 设置环境变量（根据你的 SDK 路径调整）
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk

# 4. 构建
./gradlew assembleDebug --no-daemon

# 5. 导出 APK
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/agent-app.apk
```

---

## 📸 截图

*（待添加）*

---

## 🧩 技术栈

| 组件 | 选用 |
|------|------|
| UI 框架 | Jetpack Compose + Material 3 |
| 语言 | Kotlin 2.0.21 |
| 最低 SDK | Android 8.0 (API 26) |
| 数据库 | Room (KSP) |
| 网络 | OkHttp |
| 图片 | Coil |
| 序列化 | kotlinx-serialization |
| 构建 | AGP 8.9.0 / Gradle 9.5.1 |

---

## 📄 许可

本项目仅供个人学习使用。
