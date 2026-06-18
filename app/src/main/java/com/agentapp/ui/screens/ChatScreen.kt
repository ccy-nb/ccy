package com.agentapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentapp.data.estimateChatTokens
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import com.agentapp.ui.components.ChatBubble
import com.agentapp.ui.components.CharacterAvatar
import com.agentapp.ui.components.StatusBar
import com.agentapp.ui.components.StreamingStatusBar
import com.agentapp.data.repository.VariableRepository
import com.agentapp.ui.theme.AccentBlue
import com.agentapp.ui.theme.AccentOrange
import com.agentapp.viewmodel.ChatViewModel

/**
 * 聊天界面。
 * 通过 ChatViewModel 管理状态，CuteMessageBubble / TypingIndicator 在 MessageBubble.kt，
 * MessageActionDialog / EditMessageDialog 在 ChatDialogs.kt。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    characterName: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(sessionId) {
        chatViewModel.loadSession(sessionId, characterName)
    }

    val currentSession by chatViewModel.currentSession.collectAsState()
    val inputText by chatViewModel.inputText.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val streamingText by chatViewModel.streamingText.collectAsState()
    val allSessions by chatViewModel.allSessions.collectAsState()
    val showSearch by chatViewModel.showSearch.collectAsState()
    val searchQuery by chatViewModel.searchQuery.collectAsState()
    var showMessageActions by remember { mutableStateOf<String?>(null) }
    var editDialogState by remember { mutableStateOf<Pair<String, String>?>(null) }
    var sessionExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val session = currentSession
    val matchedWorldKeywords by chatViewModel.matchedWorldKeywords.collectAsState()
    val presets by chatViewModel.presets.collectAsState()
    val selectedPresetId by chatViewModel.selectedPresetId.collectAsState()
    var presetExpanded by remember { mutableStateOf(false) }
    val varRepo = remember { VariableRepository(context) }

    // === 消息操作弹窗 ===
    if (showMessageActions != null) {
        val msgId = showMessageActions!!
        val msg = session?.messages?.find { it.id == msgId }
        MessageActionDialog(
            msgId = msgId,
            message = msg,
            onDismiss = { showMessageActions = null },
            onRegenerate = { msg?.let { chatViewModel.regenerate(it) } },
            onEdit = {
                editDialogState = msgId to (msg?.content ?: "")
            },
            onDelete = { chatViewModel.deleteMessage(msgId) },
            onBranch = if (msg?.role == Role.ASSISTANT) ({ chatViewModel.createBranch(msgId) }) else null
        )
    }

    // === 编辑消息弹窗 ===
    if (editDialogState != null) {
        val (editMsgId, editText) = editDialogState!!
        var localEditText by remember(editMsgId) { mutableStateOf(editText) }
        val editMsg = session?.messages?.find { it.id == editMsgId }
        EditMessageDialog(
            currentText = localEditText,
            onTextChange = { localEditText = it },
            onSave = {
                chatViewModel.editMessage(editMsgId, localEditText)
                editDialogState = null
            },
            onSaveAndRegenerate = if (editMsg?.role == Role.ASSISTANT) ({
                chatViewModel.editMessage(editMsgId, localEditText)
                chatViewModel.regenerate(editMsg)
                editDialogState = null
            }) else null,
            onDismiss = { editDialogState = null }
        )
    }

    // 自动滚动到底部 — 新消息或流式文本更新时跟随
    LaunchedEffect(session?.messages?.size, streamingText) {
        if (session == null || session.messages.isEmpty()) return@LaunchedEffect
        val targetIndex = if (streamingText.isNotEmpty()) {
            session.messages.size  // streaming item 在消息列表之后
        } else {
            session.messages.size - 1  // 最后一条消息
        }
        // 只在用户已经在底部附近时才自动滚动（不抢手动翻阅）
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= targetIndex - 1) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("♡ $characterName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { chatViewModel.toggleSearch() }) {
                        Text("🔍", fontSize = 16.sp)
                    }
                    IconButton(onClick = {
                        if (session != null) {
                            val sb = StringBuilder()
                            session.messages.forEach { msg ->
                                val role = if (msg.role == Role.USER) "你" else characterName
                                sb.appendLine("[$role]")
                                sb.appendLine(msg.content)
                                sb.appendLine()
                            }
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "与 $characterName 的对话")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "导出对话"))
                        }
                    }) { Text("📤", fontSize = 16.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 96.dp)  // 底部导航栏高度 + safe area，避免输入框被遮挡
            ) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { chatViewModel.setInputText(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        maxLines = 4,
                        enabled = !isLoading
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { chatViewModel.sendMessage() },
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (inputText.isNotBlank() && !isLoading) AccentOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("加载中...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { chatViewModel.loadSession(sessionId, characterName) }) {
                        Text("重试", color = AccentOrange)
                    }
                }
            }
            return@Scaffold
        }

        val showWelcome = session.messages.isEmpty() && !isLoading && streamingText.isEmpty()

        // 所有内容用 Column 垂直排列，避免重叠
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 对话切换器
            if (allSessions.size > 1) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .clickable { sessionExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "💬 对话 ${allSessions.indexOf(session) + 1}/${allSessions.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        val tokenCount = remember(session.messages.size) {
                            estimateChatTokens(session.messages)
                        }
                        Text(
                            "$tokenCount tok",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (tokenCount > 3000) AccentOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { chatViewModel.createNewSession() }) {
                            Text("＋新建", color = AccentOrange, fontSize = 13.sp)
                        }
                    }
                    DropdownMenu(
                        expanded = sessionExpanded,
                        onDismissRequest = { sessionExpanded = false }
                    ) {
                        allSessions.forEachIndexed { index, s ->
                            val preview = s.messages.firstOrNull()?.content?.take(30) ?: "空对话"
                            DropdownMenuItem(
                                text = { Text("对话 ${index + 1}: $preview", maxLines = 1) },
                                onClick = {
                                    chatViewModel.switchSession(s.id)
                                    sessionExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            val filteredMessages = if (showSearch && searchQuery.isNotBlank()) {
                session.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
            } else session.messages

            // 预设切换器
            if (presets.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
                    val currentPreset = presets.find { it.id == selectedPresetId }
                    val presetLabel = currentPreset?.let { "📋 ${it.name}" } ?: "📋 选择预设"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .clickable { presetExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(presetLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        if (currentPreset != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "T:${currentPreset.temperature}  Tok:${currentPreset.maxTokens}",
                                style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = presetExpanded,
                        onDismissRequest = { presetExpanded = false }
                    ) {
                        presets.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(
                                            "T:${p.temperature}  TopP:${p.topP}  Max:${p.maxTokens}",
                                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                onClick = {
                                    chatViewModel.selectPreset(p.id)
                                    presetExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 搜索栏
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { chatViewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    placeholder = { Text("搜索消息...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    leadingIcon = { Text("🔍", fontSize = 14.sp) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            if (showWelcome) {
                // 欢迎页
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💬", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("和 $characterName 开始聊天吧~", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.height(4.dp))
                        Text("在下方输入消息发送 ✨", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            } else {
                // 世界书关键词标签
                if (matchedWorldKeywords.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("🔖", fontSize = 12.sp)
                        matchedWorldKeywords.take(6).forEach { keyword ->
                            Text(
                                text = keyword, fontSize = 11.sp,
                                color = Color(0xFF8E7CC3), fontWeight = FontWeight.Medium,
                                modifier = Modifier.background(Color(0xFFF0ECF7), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (matchedWorldKeywords.size > 6) {
                            Text("+${matchedWorldKeywords.size - 6}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }

                // 消息列表（含浮动流式面板）
                Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(filteredMessages, key = { it.id }) { msg ->
                        val statusPanelId = remember(msg.id) {
                            val statusMarker = "```\n═══ 当前状态 ═══"
                            val statusEnd = "\n```"
                            if (msg.content.contains(statusMarker)) {
                                val startIdx = msg.content.indexOf(statusMarker)
                                val endIdx = msg.content.indexOf(statusEnd, startIdx + statusMarker.length)
                                val cleanText = if (endIdx > startIdx) {
                                    (msg.content.substring(0, startIdx) + msg.content.substring(endIdx + statusEnd.length)).trim()
                                } else msg.content.substring(0, startIdx).trim()
                                cleanText to true
                            } else {
                                msg.content to false
                            }
                        }
                        val (displayText, hasPanel) = statusPanelId

                        val isLast = msg.id == filteredMessages.lastOrNull()?.id
                        ChatBubble(
                            message = msg.copy(content = displayText),
                            characterName = characterName,
                            characterAvatarUri = if (msg.role == Role.ASSISTANT) chatViewModel.characterAvatarUri.value else null,
                            isLast = isLast,
                            onSwipeLeft = if (msg.role == Role.ASSISTANT) ({ chatViewModel.swipeLeft(msg.id) }) else {{}},
                            onSwipeRight = if (msg.role == Role.ASSISTANT) ({ chatViewModel.swipeRight(msg.id) }) else {{}},
                            onEdit = { editDialogState = msg.id to msg.content },
                            onContinue = if (msg.role == Role.ASSISTANT && isLast) ({ chatViewModel.continueMessage(msg.id) }) else null,
                            onCreateBranch = if (msg.role == Role.ASSISTANT) ({ chatViewModel.createBranch(msg.id) }) else null,
                            onDelete = { chatViewModel.deleteMessage(msg.id) }
                        )
                    }
                    if (streamingText.isNotEmpty()) {
                        item {
                            ChatBubble(
                                message = Message(role = Role.ASSISTANT, content = streamingText),
                                characterName = characterName,
                                characterAvatarUri = chatViewModel.characterAvatarUri.value,
                                isLast = true,
                                onSwipeLeft = {{}},
                                onSwipeRight = {{}},
                                onEdit = {{}},
                                onContinue = null,
                                onCreateBranch = null,
                                onDelete = {{}}
                            )
                        }
                    } else if (isLoading) {
                        item { TypingIndicator() }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                // 底部状态栏
                if (streamingText.isNotEmpty() || isLoading) {
                    StreamingStatusBar(
                        isStreaming = isLoading,
                        modelName = characterName,
                        streamingText = streamingText,
                        tokensSoFar = streamingText.length / 4,
                        onStop = { chatViewModel.cancelStream() },
                        onMinimize = {{}}
                    )
                }
            }  // close Box
            }
        }
    }
}
