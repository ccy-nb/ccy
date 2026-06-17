package com.agentapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.data.model.Character
import com.agentapp.ui.theme.AvatarColors
import com.agentapp.ui.theme.TextGray
import com.agentapp.viewmodel.CharacterListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    modifier: Modifier = Modifier,
    characterListViewModel: CharacterListViewModel
) {
    val characters by characterListViewModel.characters.collectAsState()
    val searchQuery by characterListViewModel.searchQuery.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<Character?>(null) }
    var chattingCharacterId by remember { mutableStateOf<String?>(null) }
    var chattingCharacterName by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // === 修复 #1：用 rememberLauncherForActivityResult 替代 context.startActivity ===
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                var msg = "导入失败"
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val mime = context.contentResolver.getType(uri) ?: ""
                    val ext = when {
                        mime.contains("png") -> "png"
                        mime.contains("json") -> "json"
                        else -> "png"
                    }
                    val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.$ext")
                    inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                    val char = characterListViewModel.importFromFileAndGet(tempFile)
                    if (char != null) {
                        msg = "已导入: ${char.name.ifEmpty { "未命名" }}"
                    } else if (ext == "png") {
                        msg = "此图片不包含角色卡数据"
                    }
                    tempFile.delete()
                } catch (e: Exception) {
                    msg = "导入出错: ${e.message ?: "未知"}"
                }
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    val filtered = if (searchQuery.isBlank()) characters
        else characters.filter { it.name.contains(searchQuery, ignoreCase = true) }

    // 编辑模式
    if (showEdit) {
        val char = editingCharacter ?: Character()
        CharacterEditScreen(
            character = char,
            onSave = { updated -> characterListViewModel.saveCharacter(updated); showEdit = false; editingCharacter = null },
            onCancel = { showEdit = false; editingCharacter = null }
        )
        return
    }

    // 对话模式
    if (chattingCharacterId != null) {
        ChatScreen(
            sessionId = chattingCharacterId!!,
            characterName = chattingCharacterName,
            modifier = Modifier,
            onBack = {
                chattingCharacterId = null
                chattingCharacterName = ""
            }
        )
        return
    }

    if (showUrlDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("从 URL 导入角色卡") },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUrlDialog = false
                    scope.launch(Dispatchers.IO) {
                        try {
                            val url = java.net.URL(urlInput)
                            val conn = url.openConnection()
                            conn.setRequestProperty("User-Agent", "AgentApp/1.0")
                            val bytes = conn.getInputStream().readBytes()
                            val content = String(bytes, Charsets.UTF_8)
                            characterListViewModel.importFromJsonContent(content)
                        } catch (_: Exception) { }
                    }
                }) { Text("导入", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text("取消") } },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("✨ 角色", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = {
                        importLauncher.launch(arrayOf("*/*"))
                    }) {
                        Text("导入", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showUrlDialog = true }) {
                        Text("URL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { editingCharacter = null; showEdit = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新建", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { characterListViewModel.setSearchQuery(it) },
                placeholder = { Text("搜索角色...", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFE8DDE8)
                ),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌸", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(if (searchQuery.isNotEmpty()) "没找到这个角色哦~" else "还没有角色呢~", style = MaterialTheme.typography.titleMedium, color = TextGray)
                        Text("点右上角 + 新建一个吧 ♡", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(filtered, key = { it.id }) { char ->
                        CuteCharacterCard(
                            character = char,
                            colorIndex = (char.id.hashCode() and Int.MAX_VALUE) % AvatarColors.size,
                            onClick = {
                                scope.launch {
                                    characterListViewModel.startChatWith(char.id) { sessionId, charName ->
                                        chattingCharacterId = sessionId
                                        chattingCharacterName = charName
                                    }
                                }
                            },
                            onEdit = { editingCharacter = char; showEdit = true },
                            onDelete = { characterListViewModel.deleteCharacter(char.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CuteCharacterCard(
    character: Character,
    colorIndex: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val avatarColor = AvatarColors[colorIndex]
    val initial = character.name.firstOrNull()?.toString() ?: "?"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(character.name.ifEmpty { "未命名角色" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                if (character.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(character.description, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextGray)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Create, contentDescription = "编辑", tint = TextGray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFFF9EB5), modifier = Modifier.size(20.dp))
            }
        }
    }
}
