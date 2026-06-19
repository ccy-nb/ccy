package com.agentapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.data.model.Character
import com.agentapp.ui.screens.ChatScreen
import com.agentapp.ui.screens.SettingsScreen
import com.agentapp.ui.theme.AccentOrange
import com.agentapp.ui.theme.AvatarColors
import com.agentapp.ui.theme.TextGray
import com.agentapp.viewmodel.ChatListViewModel
import com.agentapp.viewmodel.CharacterListViewModel
import com.agentapp.viewmodel.SettingsViewModel
import com.agentapp.viewmodel.WorldBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    characterListViewModel: CharacterListViewModel,
    chatListViewModel: ChatListViewModel,
    settingsViewModel: SettingsViewModel,
    worldBookViewModel: WorldBookViewModel
) {
    var showSettings by remember { mutableStateOf(false) }
    var showNewChat by remember { mutableStateOf(false) }
    var showWorldBook by remember { mutableStateOf(false) }
    var activeSessionId by remember { mutableStateOf<String?>(null) }
    var activeSessionName by remember { mutableStateOf("") }

    val allSessions by chatListViewModel.sessions.collectAsState()
    val characters by characterListViewModel.characters.collectAsState()

    // 设置
    if (showSettings) {
        if (showWorldBook) {
            com.agentapp.ui.screens.WorldBookScreen(
                worldBookViewModel = worldBookViewModel,
                onBack = { showWorldBook = false },
                showBack = true
            )
        } else {
            SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                settingsViewModel = settingsViewModel,
                characterListViewModel = characterListViewModel,
                onNavigateToWorldBook = { showWorldBook = true },
                onBack = { showSettings = false }
            )
        }
        return
    }

    // 新建对话弹窗
    if (showNewChat) {
        CharacterPickerDialog(
            characters = characters,
            onSelect = { char ->
                showNewChat = false
                characterListViewModel.createNewSession(char.id) { sessionId, name ->
                    activeSessionId = sessionId
                    activeSessionName = name
                }
            },
            onDismiss = { showNewChat = false }
        )
    }

    // 聊天界面
    if (activeSessionId != null) {
        ChatScreen(
            sessionId = activeSessionId!!,
            characterName = activeSessionName,
            onBack = {
                activeSessionId = null
                activeSessionName = ""
                chatListViewModel.refresh()
            }
        )
        return
    }

    // 主界面
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Agent App", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChat = true },
                containerColor = AccentOrange,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建对话")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (allSessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无对话", fontSize = 16.sp, color = TextGray)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showNewChat = true }) {
                        Text("＋ 新建对话", color = AccentOrange)
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                items(allSessions, key = { it.first.id }) { (session, charName) ->
                    val initial = charName.firstOrNull()?.toString() ?: "?"
                    val colorIdx = kotlin.math.abs(charName.hashCode()) % AvatarColors.size
                    val avatarColor = AvatarColors[colorIdx]

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            activeSessionId = session.id
                            activeSessionName = charName
                        },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(1.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(avatarColor, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(charName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${session.messages.size} 条消息", fontSize = 12.sp, color = TextGray)
                            }
                            if (session.parentSessionId != null) {
                                Text("🌿", fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                            }
                            IconButton(onClick = { chatListViewModel.deleteSession(session.id) },
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterPickerDialog(
    characters: List<Character>,
    onSelect: (Character) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择角色", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                items(characters) { char ->
                    val initial = char.name.firstOrNull()?.toString() ?: "?"
                    val colorIdx = kotlin.math.abs(char.name.hashCode()) % AvatarColors.size
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(char) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(AvatarColors[colorIdx], CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(char.name, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
