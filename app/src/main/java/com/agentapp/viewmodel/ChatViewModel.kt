package com.agentapp.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentapp.data.api.ApiFactory
import com.agentapp.data.model.ChatSession
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import com.agentapp.data.model.WorldEntryPosition
import com.agentapp.data.repository.CharacterRepository
import com.agentapp.data.repository.ChatRepository
import com.agentapp.data.repository.SettingsRepository
import com.agentapp.data.repository.WorldRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val chatRepo = ChatRepository(application)
    private val characterRepo = CharacterRepository(application)
    private val settingsRepo = SettingsRepository(application)
    private val worldRepo = WorldRepository(application)
    private val personaRepo = com.agentapp.data.repository.PersonaRepository(application)
    private val regexRepo = com.agentapp.data.repository.RegexRepository(application)
    private val varRepo = com.agentapp.data.repository.VariableRepository(application)
    private val apiFactory = ApiFactory()

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    private val _characterName = MutableStateFlow("")
    val characterName: StateFlow<String> = _characterName.asStateFlow()

    private val _allSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val allSessions: StateFlow<List<ChatSession>> = _allSessions.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _matchedWorldKeywords = MutableStateFlow<List<String>>(emptyList())
    val matchedWorldKeywords: StateFlow<List<String>> = _matchedWorldKeywords.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch.asStateFlow()

    private var messagesJob: Job? = null
    private var streamJob: Job? = null  // 流式请求协程引用
    private val sendMutex = Mutex()     // 防发送竞态

    private var ttsInstance: TextToSpeech? = null

    fun loadSession(sessionId: String, charName: String) {
        _characterName.value = charName
        viewModelScope.launch {
            val session = chatRepo.get(sessionId) ?: return@launch
            _currentSession.value = session
            _allSessions.value = chatRepo.list(session.characterId)

            // 监听消息变化
            messagesJob?.cancel()
            messagesJob = viewModelScope.launch {
                chatRepo.getMessagesFlow(sessionId).collect { messages ->
                    _currentSession.value = _currentSession.value?.copy(messages = messages)
                }
            }
        }
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun toggleSearch() {
        _showSearch.value = !_showSearch.value
        if (!_showSearch.value) _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        _inputText.value = ""

        val session = _currentSession.value ?: return
        val userMsg = Message(role = Role.USER, content = text)
        val updatedMessages = session.messages + userMsg
        doSendMessages(updatedMessages)
    }

    fun regenerate(msg: Message) {
        val session = _currentSession.value ?: return
        val idx = session.messages.indexOf(msg)
        if (idx <= 0) return
        val userMsg = session.messages[idx - 1]
        val filtered = session.messages.filterIndexed { i, _ -> i != idx && i != idx - 1 }
        doSendMessages(filtered + userMsg)
    }

    fun editMessage(msgId: String, newContent: String) {
        val session = _currentSession.value ?: return
        val updated = session.messages.map { m ->
            if (m.id == msgId) m.copy(content = newContent) else m
        }
        _currentSession.value = session.copy(messages = updated)
        // 暂停 Flow 收集，保存后再通过 Flow 同步
        messagesJob?.cancel()
        viewModelScope.launch {
            chatRepo.save(_currentSession.value!!)
            // 重启 Flow 收集
            val sid = _currentSession.value?.id ?: return@launch
            messagesJob = viewModelScope.launch {
                chatRepo.getMessagesFlow(sid).collect { messages ->
                    _currentSession.value = _currentSession.value?.copy(messages = messages)
                }
            }
        }
    }

    /** 编辑消息内容并从编辑点重新生成 AI 回复 */
    fun editAndRegenerate(msgId: String, newContent: String) {
        val session = _currentSession.value ?: return
        val editedIdx = session.messages.indexOfFirst { it.id == msgId }
        if (editedIdx < 0) return

        val updatedMessages = session.messages.mapIndexed { i, m ->
            if (i == editedIdx) m.copy(content = newContent) else m
        }
        val editedMsg = updatedMessages[editedIdx]

        val messagesToSend = when (editedMsg.role) {
            Role.USER -> updatedMessages.subList(0, editedIdx + 1)
            Role.ASSISTANT -> {
                if (editedIdx <= 0) return
                val userMsg = updatedMessages[editedIdx - 1]
                updatedMessages.filterIndexed { i, _ -> i != editedIdx && i != editedIdx - 1 } + userMsg
            }
            else -> return
        }

        messagesJob?.cancel()
        doSendMessages(messagesToSend)
        // 重新监听消息变化
        viewModelScope.launch {
            val sid = _currentSession.value?.id ?: return@launch
            messagesJob = viewModelScope.launch {
                chatRepo.getMessagesFlow(sid).collect { messages ->
                    _currentSession.value = _currentSession.value?.copy(messages = messages)
                }
            }
        }
    }

    fun deleteMessage(msgId: String) {
        val session = _currentSession.value ?: return
        val filtered = session.messages.filter { it.id != msgId }
        _currentSession.value = session.copy(messages = filtered)
        viewModelScope.launch {
            chatRepo.save(_currentSession.value!!)
        }
    }

    fun switchSession(sessionId: String) {
        // 取消旧 session 的流式请求和消息监听
        streamJob?.cancel()
        messagesJob?.cancel()
        viewModelScope.launch {
            val session = chatRepo.get(sessionId) ?: return@launch
            _currentSession.value = session
            _isLoading.value = false
            _streamingText.value = ""
            messagesJob = viewModelScope.launch {
                chatRepo.getMessagesFlow(sessionId).collect { messages ->
                    _currentSession.value = _currentSession.value?.copy(messages = messages)
                }
            }
        }
    }

    fun createNewSession() {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            val ns = chatRepo.create(session.characterId)
            _allSessions.value = chatRepo.list(session.characterId)
            switchSession(ns.id)
        }
    }

    fun refreshSessions() {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            _allSessions.value = chatRepo.list(session.characterId)
        }
    }

    fun speakText(text: String) {
        val ctx = getApplication<Application>()
        if (ttsInstance == null) {
            ttsInstance = TextToSpeech(ctx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsInstance?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        } else {
            ttsInstance?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdownTts() {
        ttsInstance?.stop()
        ttsInstance?.shutdown()
        ttsInstance = null
    }

    private fun doSendMessages(messages: List<Message>) {
        val session = _currentSession.value ?: return
        val updated = session.copy(messages = messages)
        _currentSession.value = updated

        viewModelScope.launch {
            chatRepo.save(updated)
        }

        // 取消上一次流式请求
        streamJob?.cancel()
        _isLoading.value = true
        _streamingText.value = ""
        _matchedWorldKeywords.value = emptyList()

        streamJob = viewModelScope.launch {
            sendMutex.withLock {
                val builder = StringBuilder()
                val baseScripts = regexRepo.list().toMutableList()  // 基础正则
                // 加载当前变量 → 生成状态面板
                val currentVars = varRepo.get(session.id)
                val statusText = if (currentVars.keys.isNotEmpty()) {
                    com.agentapp.data.model.formatVariableTree(varRepo.flattenVariables(currentVars))
                } else ""
                val scripts = baseScripts.toMutableList()
                if (statusText.isNotEmpty()) {
                    val idx = scripts.indexOfFirst { it.findRegex.contains("StatusPlaceHolder") }
                    if (idx >= 0) scripts[idx] = scripts[idx].copy(replaceString = "```\n$statusText\n```")
                }

                try {
                    val cfg = settingsRepo.getApiConfigSync()
                    val character = characterRepo.get(session.characterId)

                    // 构建带世界书的 API 消息列表
                    val apiMsgs = buildApiMessages(updated.messages, character)

                    apiFactory.chatStream(cfg, apiMsgs).collect { chunk ->
                        builder.append(chunk)
                        _streamingText.value = regexRepo.applyScripts(builder.toString(), scripts)
                    }

                    val reply = builder.toString()
                    if (reply.isNotEmpty()) {
                        val s = _currentSession.value ?: return@withLock
                        if (reply.startsWith("[ERROR:")) {
                            val raw = reply.removePrefix("[ERROR: ").removeSuffix("]").trim()
                            val errText = if (raw.isEmpty() || raw == "null") "未知错误" else raw
                            val em = Message(role = Role.ASSISTANT, content = "⚠️ API 错误：$errText")
                            val fs = s.copy(messages = s.messages + em)
                            withContext(Dispatchers.Main) { _currentSession.value = fs }
                            chatRepo.save(fs)
                        } else {
                            val processedReply = regexRepo.applyScripts(reply, scripts)
                            val am = Message(role = Role.ASSISTANT, content = processedReply)
                            val fs = s.copy(messages = s.messages + am)

                            // 从回复中提取变量更新（异步，不阻塞渲染）
                            val patchOps = com.agentapp.data.model.parseUpdateVariableBlock(reply)
                            if (patchOps.isNotEmpty()) {
                                viewModelScope.launch {
                                    val newVars = varRepo.applyPatch(currentVars, patchOps)
                                    varRepo.save(session.id, newVars)
                                }
                            }
                            withContext(Dispatchers.Main) { _currentSession.value = fs }
                            chatRepo.save(fs)
                        }
                    }
                    _streamingText.value = ""
                } catch (e: kotlinx.coroutines.CancellationException) {
                    if (builder.isNotEmpty()) {
                        val s = _currentSession.value ?: return@withLock
                        val partial = regexRepo.applyScripts(builder.toString(), scripts) + "\n\n(回复未完成)"
                        val pm = Message(role = Role.ASSISTANT, content = partial)
                        val fs = s.copy(messages = s.messages + pm)
                        withContext(Dispatchers.Main) { _currentSession.value = fs }
                        chatRepo.save(fs)
                    }
                    _streamingText.value = ""
                    throw e
                } catch (e: Exception) {
                    val detail = when {
                        e is java.net.UnknownHostException -> "无法连接服务器，请检查网络和 API 地址"
                        e is java.net.SocketTimeoutException -> "连接超时，请检查网络和 API 地址"
                        e is java.io.IOException && e.message != null -> e.message!!
                        else -> {
                            val msg = e.message ?: ""
                            val cls = e.javaClass.simpleName
                            val cause = e.cause?.let { " ← ${it.javaClass.simpleName}: ${it.message ?: ""}" } ?: ""
                            if (msg.isNotBlank()) msg else "$cls$cause"
                        }
                    }
                    val s = _currentSession.value ?: return@withLock
                    val em = Message(role = Role.ASSISTANT, content = "⚠️ 出错了：$detail")
                    val fs = s.copy(messages = s.messages + em)
                    withContext(Dispatchers.Main) { _currentSession.value = fs }
                    chatRepo.save(fs)
                }
                _isLoading.value = false
            }
        }
    }

    /**
     * 构建发送给 API 的消息列表，集成世界书逻辑：
     * - 检查角色总开关（worldBookEnabled）
     * - 按角色 ID 匹配世界书条目
     * - 按 position 字段插入到对应位置
     * - 同时更新 _matchedWorldKeywords 供 UI 展示
     */
    private suspend fun buildApiMessages(messages: List<Message>, character: com.agentapp.data.model.Character?): List<Message> {
        val session = _currentSession.value ?: return if (character != null) {
            listOf(Message(role = Role.SYSTEM, content = character.buildSystemPrompt())) + messages
        } else messages

        // 检查角色总开关
        val worldBookEnabled = character?.worldBookEnabled ?: true

        if (!worldBookEnabled) {
            val sysMsg = character?.let {
                Message(role = Role.SYSTEM, content = it.buildSystemPrompt())
            }
            return if (sysMsg != null) listOf(sysMsg) + messages else messages
        }

        // 匹配世界书
        val matchResult = worldRepo.matchEntries(messages, character?.id)
        val matchedKeys = worldRepo.matchKeywords(messages, character?.id)
        _matchedWorldKeywords.value = matchedKeys

        // 构建 system prompt 内容
        val sysContent = StringBuilder()

        // BEFORE_SYSTEM 条目插入最前面
        matchResult[WorldEntryPosition.BEFORE_SYSTEM]?.forEach { entry ->
            sysContent.appendLine(entry.content)
            sysContent.appendLine()
        }

        // 用户 Persona（失败不影响聊天）
        try {
            val persona = personaRepo.get()
            val personaPrompt = persona.buildPrompt()
            if (personaPrompt.isNotEmpty()) {
                sysContent.appendLine(personaPrompt)
                sysContent.appendLine()
            }
        } catch (_: Exception) { /* 忽略 Persona 加载失败 */ }

        // 角色定义
        if (character != null) {
            sysContent.append(character.buildCharacterPrompt())
            sysContent.appendLine()
        }

        // 深度提示（独立注入，在角色定义之后、世界书 AFTER_SYSTEM 之前）
        if (character != null && character.depthPrompt.isNotBlank()) {
            sysContent.appendLine("=== 深度指令 ===")
            sysContent.appendLine(character.depthPrompt)
            sysContent.appendLine()
        }

        // AFTER_SYSTEM 条目追加到最后
        matchResult[WorldEntryPosition.AFTER_SYSTEM]?.forEach { entry ->
            sysContent.appendLine()
            sysContent.appendLine("=== 世界观设定 ===")
            sysContent.appendLine(entry.content)
        }

        val sysMsg = Message(role = Role.SYSTEM, content = sysContent.toString())
        val apiMsgs = mutableListOf(sysMsg).apply { addAll(messages) }

        // BEFORE_USER 条目插入到用户最后一条消息之前
        val beforeUserEntries = matchResult[WorldEntryPosition.BEFORE_USER]
        if (!beforeUserEntries.isNullOrEmpty()) {
            val beforeUserContent = beforeUserEntries.joinToString("\n") { it.content }
            val beforeUserMsg = Message(role = Role.SYSTEM, content = beforeUserContent)
            apiMsgs.add(apiMsgs.size - 1, beforeUserMsg)
        }

        return apiMsgs
    }

    override fun onCleared() {
        super.onCleared()
        shutdownTts()
    }
}
