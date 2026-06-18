# Agent App — 角色扮演聊天客户端

## 一句话
多 API 角色扮演聊天 App，Compose + Material 3，Termux 开发环境。

## 技术栈
- Kotlin + Jetpack Compose + Material 3（固定粉紫暖色调）
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
│   ├── components/          # MarkdownText, StatusPanel, V3StylePanel
│   ├── screens/             # ChatScreen, ChatListScreen, SettingsScreen...
│   └── theme/               # Theme.kt 粉紫配色
└── viewmodel/               # ChatViewModel, ChatListViewModel...
```

## 当前状态 (2026-06-17)

### 已实现
- 多 API 兼容（OpenAI + Claude），流式 chatCompletion callbackFlow
- API 测试连接（GET /models → 模型下拉）
- 后台协程（Application scope，切 Tab 不中断）
- 角色列表 + 聊天列表 + 聊天界面
- 正则替换引擎（Import regex_scripts → 消息管道）
- 变量系统（JSONPatch 解析 + StatusPlaceHolder）
- StatusPanel + V3StylePanel 前端卡片
- 深度提示独立注入（角色→深度指令→世界书 AFTER_SYSTEM）
- 世界书三级结构（角色→世界书→条目），酒馆风格 UI
- 世界书空关键词=始终触发（酒馆 constant 逻辑）
- 粉紫 Material 3 固定配色
- Room 本地持久化
- 设置页（API Key / Base URL / 模型选择 / 预设 / 主题）

### 上次提交
`7e698bb` 前端面板：v3_style导入+WebView渲染 + V3StylePanel组件

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
- Termux aapt2 不兼容 — gradle.properties 有 override
- 流式读取必须 `close()` 否则 `collect` 永不返回
- `CancellationException` 不吞掉，保存部分回复后 rethrow

## 会话日志

每次工作结束时追加。格式：`YYYY-MM-DD | 做了什么 | git commit hash | 关键决策`

| 日期 | 变更 | Commit | 备注 |
|------|------|--------|------|
| 2026-06-17 | 初始化会话日志机制 + 自动保存工作流 | — | 新增 verify-before-git-push 记忆加入「保存项目状态」步骤；REASONIX_CONTEXT.md 底部追加会话日志区 |
| 2026-06-18 | 修复流式生成长文本不自动滚动 | `f987a86` | LaunchedEffect 增加 streamingText 依赖 + 已读保护；全局记忆 auto-export-apk |
| 2026-06-18 | 字数控制 + 预设系统全面改造 | `1c7e5f1` | Preset/ApiConfig 扩展采样参数；ChatViewModel 合并预设；上下文截断；ChatScreen 预设切换；Persona 扩展 |
| 2026-06-18 | 酒馆设置界面调研 | — | 研究 SillyTavern 9抽屉/折叠/内联编辑设计哲学；项目记忆 silly-tavern-settings-design |
| 2026-06-19 | 对话界面改造一期 — Swipe/头像/内联编辑/导入 | `a22ce4e` | MessageEntity 新增 parentMessageId/siblingIndex 支持 swipe；MessageBubble 新增 swipe 控件/头像/内联编辑；SettingsScreen 新增预设导入导出 + 世界书导入（ST lorebook 格式兼容）；WorldEntryPosition 扩展 6 个位置；Room MIGRATION_5_6 |
| 2026-06-19 | 对话界面改造二期 — Continue + 浮动流式面板 | `0c17820` | MessageBubble 新增 Continue（↘按钮）+ onContinue 回调；ChatViewModel 新增 continueMessage()（追加到已有消息）；新增 StreamingPanel 浮动面板（LED 状态灯/停止/最小化）；ChatScreen Box 叠加流式面板 |
| 2026-06-19 | 对话界面改造三期 — 分支对话 | TBD | ChatRepository.branchSession() 复制消息到新会话；ChatViewModel.createBranch() 自动切换；消息长按菜单「🌿 从此创建分支」；ChatListScreen 分支缩进+🌿标记；ChatSession 新增 parentSessionId；MIGRATION_6_7 |

> **下次工作流程：** 读此表 + 读 `remember` 项目记忆 → 快速恢复上下文 → 继续开发。
