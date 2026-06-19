# Agent App — 角色扮演聊天客户端

## 技术栈
- Kotlin + Jetpack Compose + Material 3（SillyTavern 风格双色主题）
- AGP 8.9.0 / Gradle 9.5.1 / Kotlin 2.0.21
- compileSdk 34, minSdk 26, targetSdk 34
- Room (KSP) + DataStore + OkHttp + Coil + kotlinx-serialization

## API 兼容层
```
OpenAI 格式 → OpenAiClient（DeepSeek/OpenAI/NVIDIA/Gemini/Groq/自定义）
Anthropic  → ClaudeClient（独立）
ApiFactory 通过 isOpenAiCompatible() 分发
```

## 关键文件

```
app/src/main/java/com/agentapp/
├── AgentApp.kt              # Application + appScope 后台协程
├── AgentAppNavHost.kt       # 导航
├── MainActivity.kt          # 入口 enableEdgeToEdge()
├── MainScreen.kt            # 主框架 Scaffold + 底部导航
├── data/
│   ├── api/                 # ApiFactory, OpenAiClient, ClaudeClient
│   ├── local/               # AppDatabase, DAO, Entity (Room)
│   ├── model/               # 数据模型（Character, Message, WorldBook...）
│   └── repository/          # 仓库层
├── ui/
│   ├── components/          # ChatBubble, StatusBar, StreamingPanel, MarkdownText
│   ├── screens/             # ChatScreen, ChatListScreen, SettingsScreen...
│   └── theme/               # Theme.kt 深蓝灰/米白双色
└── viewmodel/               # ChatViewModel, ChatListViewModel...
```

## 构建命令

```bash
cd ~/agent\ app
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk
./gradlew assembleDebug --no-daemon
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/agent-app.apk
```

## 坑
- 项目文件夹名带空格 — `cd ~/agent\ app`
- Termux aapt2 不兼容 — `local.properties` 有 `android.aapt2FromMavenOverride`（不提交 Git，CI 不受影响）
- 流式读取必须 `close()` 否则 `collect` 永不返回
- `CancellationException` 不吞掉，保存部分回复后 rethrow

## 记忆与状态

> 实时状态、功能清单、开发记录 → 知识图谱（`agent-app` 实体 + `session` 实体）
> 完整 Git 历史 → `git log --oneline`
