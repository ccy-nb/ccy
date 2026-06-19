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
import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Preset
import com.agentapp.data.repository.PresetRepository
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
    private val presetRepo = PresetRepository(application)
    private val apiFactory = ApiFactory()
    private val contextManager = com.agentapp.engine.ContextManager(
        worldRepo = worldRepo,
        personaProvider = { personaRepo.get() }
    )

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    private val _characterName = MutableStateFlow("")
    val characterName: StateFlow<String> = _characterName.asStateFlow()

    private val _characterAvatarUri = MutableStateFlow<String?>(null)
    val characterAvatarUri: StateFlow<String?> = _characterAvatarUri.asStateFlow()

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

    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    private val _selectedPresetId = MutableStateFlow<String?>(null)
    val selectedPresetId: StateFlow<String?> = _selectedPresetId.asStateFlow()

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

            // 加载角色头像
            val character = characterRepo.get(session.characterId)
            _characterAvatarUri.value = character?.avatarUri

            // 加载预设列表
            _presets.value = presetRepo.list()
            // 尝试选中第一个预设
            if (_selectedPresetId.value == null && _presets.value.isNotEmpty()) {
                _selectedPresetId.value = _presets.value.first().id
            }

            // 监听消息变化
            messagesJob?.cancel()
            messagesJob = viewModelScope.launch {
                chatRepo.getMessagesFlow(sessionId).collect { messages ->
                    _currentSession.value = _currentSession.value?.copy(messages = messages)
                }
            }
        }
    }

    /** swipe 到上一个版本 */
    fun swipeLeft(msgId: String) {
        val session = _currentSession.value ?: return
        val idx = session.messages.indexOfFirst { it.id == msgId }
        if (idx < 0) return
        val msg = session.messages[idx]
        if (msg.swipes.isEmpty() || msg.currentSwipeId <= 0) return
        val newSwipeId = msg.currentSwipeId - 1
        val updatedMsg = msg.copy(
            content = msg.swipes[newSwipeId],
            currentSwipeId = newSwipeId
        )
        val updatedMessages = session.messages.toMutableList().apply { set(idx, updatedMsg) }
        _currentSession.value = session.copy(messages = updatedMessages)
        // 持久化当前选择的版本
        viewModelScope.launch { chatRepo.save(_currentSession.value!!) }
    }

    /** swipe 到下一个版本。如果已经在最后一个版本，触发 regenerate 生成新版本 */
    fun swipeRight(msgId: String) {
        val session = _currentSession.value ?: return
        val idx = session.messages.indexOfFirst { it.id == msgId }
        if (idx < 0) return
        val msg = session.messages[idx]
        if (msg.swipes.isNotEmpty() && msg.currentSwipeId < msg.swipes.size - 1) {
            // 切换到下一个已有版本
            val newSwipeId = msg.currentSwipeId + 1
            val updatedMsg = msg.copy(
                content = msg.swipes[newSwipeId],
                currentSwipeId = newSwipeId
            )
            val updatedMessages = session.messages.toMutableList().apply { set(idx, updatedMsg) }
            _currentSession.value = session.copy(messages = updatedMessages)
            viewModelScope.launch { chatRepo.save(_currentSession.value!!) }
        } else if (msg.role == Role.ASSISTANT) {
            // 没有更多版本了 → regenerate 生成一个新版本
            regenerate(msg)
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

    /**
     * 重新生成 AI 回复。如果是最后一条 AI 消息，新版本会作为 swipe 添加（不覆盖）；
     * 如果是历史消息，则直接替换（保持向后兼容）。
     */
    fun regenerate(msg: Message) {
        val session = _currentSession.value ?: return
        val idx = session.messages.indexOf(msg)
        if (idx <= 0) return
        val userMsg = session.messages[idx - 1]
        val isLastAi = idx == session.messages.size - 1

        if (isLastAi && msg.swipes.isNotEmpty()) {
            // 最后一条 AI 消息已有多个版本 → 生成新版本作为 swipe
            val filtered = session.messages.filterIndexed { i, _ -> i != idx }
            doSendMessages(filtered + userMsg, swipeParentId = msg.id)
        } else if (isLastAi && msg.role == Role.ASSISTANT) {
            // 最后一条 AI 消息但还没有 swipe → 首次 swipe，保留原版
            val filtered = session.messages.filterIndexed { i, _ -> i != idx }
            doSendMessages(filtered + userMsg, swipeParentId = msg.id, firstSwipeContent = msg.content)
        } else {
            // 历史消息 → 直接替换（不产生 swipe 版本）
            val filtered = session.messages.filterIndexed { i, _ -> i != idx && i != idx - 1 }
            doSendMessages(filtered + userMsg)
        }
    }

    /** 从指定消息创建分支会话 */
    fun createBranch(fromMessageId: String) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            val branch = chatRepo.branchSession(session.characterId, session.id, fromMessageId)
            if (branch != null) {
                // 刷新会话列表
                _allSessions.value = chatRepo.list(session.characterId)
                // 自动切换到新分支
                switchSession(branch.id)
            }
        }
    }

    /** 继续生成：在最后一条 AI 消息后追加内容，不创建新消息 */
    fun continueMessage(msgId: String) {
        val session = _currentSession.value ?: return
        val idx = session.messages.indexOfFirst { it.id == msgId }
        if (idx < 0 || idx != session.messages.size - 1) return  // 只能继续最后一条
        val msg = session.messages[idx]
        if (msg.role != Role.ASSISTANT) return
        // 从消息列表中移除这条 AI 消息（已有内容作为 API 上下文的一部分）
        val filtered = session.messages.filterIndexed { i, _ -> i != idx }
        // 在发送时带上全部上下文（包括已有的 AI 回复内容），新回复追加到原内容后
        doSendMessages(filtered, continueTargetId = msgId, continuePrefix = msg.content)
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
            // 刷新预设列表
            _presets.value = presetRepo.list()
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

    fun selectPreset(presetId: String?) {
        _selectedPresetId.value = presetId
    }

    /** 合并预设参数到 ApiConfig：预设值覆盖全局配置 */
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

    fun cancelStream() {
        streamJob?.cancel()
        _isLoading.value = false
        _streamingText.value = ""
    }

    fun shutdownTts() {
        ttsInstance?.stop()
        ttsInstance?.shutdown()
        ttsInstance = null
    }

    /**
     * @param swipeParentId 非 null 时，新生成的 AI 回复作为该消息的 swipe 版本
     * @param firstSwipeContent swipe 首次生成时，原消息内容作为第一个版本
     */
    private fun doSendMessages(
        messages: List<Message>,
        swipeParentId: String? = null,
        firstSwipeContent: String? = null,
        continueTargetId: String? = null,
        continuePrefix: String? = null
    ) {
        val session = _currentSession.value ?: return
        val updated = session.copy(messages = messages)
        _currentSession.value = updated

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
                    com.agentapp.data.model.formatVariableTree(com.agentapp.data.model.flattenVariables(currentVars))
                } else ""
                val scripts = baseScripts.toMutableList()
                if (statusText.isNotEmpty()) {
                    val idx = scripts.indexOfFirst { it.findRegex.contains("StatusPlaceHolder") }
                    if (idx >= 0) scripts[idx] = scripts[idx].copy(replaceString = "```\n$statusText\n```")
                }

                try {
                    val baseCfg = settingsRepo.getApiConfigSync()
                    val cfg = contextManager.mergePreset(baseCfg, _presets.value, _selectedPresetId.value)
                    val character = characterRepo.get(session.characterId)

                    // 构建带世界书的 API 消息列表
                    val apiMsgs = contextManager.buildApiMessages(
                        messages = updated.messages,
                        character = character,
                        characterId = session.characterId,
                        onMatchedKeywords = { _matchedWorldKeywords.value = it }
                    )
                    // 按上下文窗口截断
                    val truncatedMsgs = contextManager.truncateMessages(apiMsgs, cfg)

                    apiFactory.chatStream(cfg, truncatedMsgs).collect { chunk ->
                        builder.append(chunk)
                        // 流式显示原始文本，正则只在最终结果应用一次
                        _streamingText.value = builder.toString()
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

                            if (continueTargetId != null) {
                                // Continue 模式：新回复追加到已有消息末尾
                                val targetMsg = s.messages.find { it.id == continueTargetId }
                                if (targetMsg != null) {
                                    val fullContent = (continuePrefix ?: targetMsg.content) + "\n\n" + processedReply
                                    val updatedMsg = targetMsg.copy(content = fullContent)
                                    val updatedMessages = s.messages.map { if (it.id == continueTargetId) updatedMsg else it }
                                    val fs = s.copy(messages = updatedMessages)
                                    withContext(Dispatchers.Main) { _currentSession.value = fs }
                                    chatRepo.save(fs)
                                }
                            } else if (swipeParentId != null) {
                                // Swipe 模式：新回复作为已有消息的额外版本
                                val parentMsg = s.messages.find { it.id == swipeParentId }
                                if (parentMsg != null) {
                                    val existingSwipes = parentMsg.swipes.toMutableList()
                                    // 如果是首次 swipe，把原有内容作为第一个版本
                                    if (firstSwipeContent != null && existingSwipes.isEmpty()) {
                                        existingSwipes.add(firstSwipeContent)
                                    }
                                    val newSwipeId = existingSwipes.size
                                    existingSwipes.add(processedReply)
                                    val updatedMsg = parentMsg.copy(
                                        content = processedReply,
                                        swipes = existingSwipes,
                                        currentSwipeId = newSwipeId
                                    )
                                    val updatedMessages = s.messages.map { if (it.id == swipeParentId) updatedMsg else it }
                                    val fs = s.copy(messages = updatedMessages)
                                    withContext(Dispatchers.Main) { _currentSession.value = fs }
                                    chatRepo.save(fs)
                                    // 同时保存 swipe 版本到 Room（用 addSwipe 存独立行）
                                    chatRepo.addSwipe(session.id, swipeParentId, processedReply)
                                }
                            } else {
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
                    }
                    _streamingText.value = ""
                } catch (e: kotlinx.coroutines.CancellationException) {
                    if (builder.isNotEmpty()) {
                        val s = _currentSession.value ?: return@withLock
                        val partial = regexRepo.applyScripts(builder.toString(), scripts) + "\n\n(回复未完成)"
                        if (continueTargetId != null) {
                            // Cancelled continue: 追加到已有消息末尾
                            val targetMsg = s.messages.find { it.id == continueTargetId }
                            if (targetMsg != null) {
                                val fullContent = (continuePrefix ?: targetMsg.content) + "\n\n" + partial
                                val updatedMsg = targetMsg.copy(content = fullContent)
                                val updatedMessages = s.messages.map { if (it.id == continueTargetId) updatedMsg else it }
                                val fs = s.copy(messages = updatedMessages)
                                withContext(Dispatchers.Main) { _currentSession.value = fs }
                                chatRepo.save(fs)
                            }
                        } else if (swipeParentId != null) {
                            // Cancelled swipe: 保存部分内容作为新版本
                            val parentMsg = s.messages.find { it.id == swipeParentId }
                            if (parentMsg != null) {
                                val existingSwipes = parentMsg.swipes.toMutableList()
                                val newSwipeId = existingSwipes.size
                                existingSwipes.add(partial)
                                val updatedMsg = parentMsg.copy(swipes = existingSwipes, currentSwipeId = newSwipeId, content = partial)
                                val updatedMessages = s.messages.map { if (it.id == swipeParentId) updatedMsg else it }
                                val fs = s.copy(messages = updatedMessages)
                                withContext(Dispatchers.Main) { _currentSession.value = fs }
                                chatRepo.save(fs)
                            }
                        } else {
                            val pm = Message(role = Role.ASSISTANT, content = partial)
                            val fs = s.copy(messages = s.messages + pm)
                            withContext(Dispatchers.Main) { _currentSession.value = fs }
                            chatRepo.save(fs)
                        }
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
    override fun onCleared() {
        streamJob?.cancel()
        messagesJob?.cancel()
        super.onCleared()
        shutdownTts()
    }
}
