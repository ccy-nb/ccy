@file:Suppress("DEPRECATION")

package com.agentapp.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.data.model.Character
import com.agentapp.data.model.WorldEntry
import com.agentapp.data.model.WorldEntryPosition
import com.agentapp.ui.theme.Pink
import com.agentapp.ui.theme.PinkDark
import com.agentapp.ui.theme.PinkLight
import com.agentapp.ui.theme.TextGray
import com.agentapp.ui.theme.WarmWhite
import com.agentapp.viewmodel.WorldBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBookScreen(
    worldBookViewModel: WorldBookViewModel,
    onBack: () -> Unit,
    showBack: Boolean = true
) {
    val entries by worldBookViewModel.entries.collectAsState()
    val characters by worldBookViewModel.characters.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<WorldEntry?>(null) }

    if (showAdd || editEntry != null) {
        WorldEntryEditScreen(
            entry = editEntry ?: WorldEntry(),
            characters = characters,
            onSave = { worldBookViewModel.saveEntry(it); showAdd = false; editEntry = null },
            onCancel = { showAdd = false; editEntry = null }
        )
        return
    }

    Scaffold(
        containerColor = WarmWhite,
        topBar = {
            TopAppBar(
                title = { Text("📖 世界书", fontWeight = FontWeight.Bold) },
                navigationIcon = { if (showBack) IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) } },
                actions = { IconButton(onClick = { showAdd = true }) { Text("＋", fontSize = 20.sp) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("还没有世界观设定", style = MaterialTheme.typography.titleMedium, color = TextGray)
                    Text("添加后对话中会自动触发", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                items(entries, key = { it.id }) { entry ->
                    WorldEntryCard(entry = entry, onEdit = { editEntry = entry }, onDelete = { worldBookViewModel.deleteEntry(entry.id) })
                }
            }
        }
    }
}

@Composable
fun WorldEntryCard(entry: WorldEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val positionLabel = when (entry.position) {
        WorldEntryPosition.BEFORE_SYSTEM -> "📌 最前"
        WorldEntryPosition.AFTER_SYSTEM -> "📎 最后"
        WorldEntryPosition.BEFORE_USER -> "💬 用户前"
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("关键词: ${entry.keys.joinToString(", ")}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("#${entry.priority}", fontSize = 12.sp, color = Pink, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(positionLabel, fontSize = 11.sp, color = Color(0xFF8E7CC3), fontWeight = FontWeight.Medium)
                if (entry.probability < 1.0f) {
                    Text("🎲 ${(entry.probability * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF8E7CC3))
                }
                if (!entry.enabled) {
                    Text("🚫 已禁用", fontSize = 11.sp, color = Color(0xFFFF6B8A))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(entry.content.take(100), fontSize = 13.sp, color = TextGray, maxLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("编辑", color = Pink) }
                TextButton(onClick = onDelete) { Text("删除", color = Color(0xFFFF6B8A)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldEntryEditScreen(
    entry: WorldEntry,
    characters: List<Character>,
    onSave: (WorldEntry) -> Unit,
    onCancel: () -> Unit
) {
    var keys by remember { mutableStateOf(entry.keys.joinToString(", ")) }
    var content by remember { mutableStateOf(entry.content) }
    var enabled by remember { mutableStateOf(entry.enabled) }
    var priorityText by remember { mutableStateOf(entry.priority.toString()) }
    var probability by remember { mutableStateOf(entry.probability) }
    var position by remember { mutableStateOf(entry.position) }
    var selectedCharId by remember { mutableStateOf(entry.characterId) }

    var positionExpanded by remember { mutableStateOf(false) }
    var charExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val selectedCharName = if (selectedCharId == null) "🌐 全局" else characters.find { it.id == selectedCharId }?.name ?: "🌐 全局"

    // 检查是否有未保存修改
    fun hasChanges(): Boolean {
        return keys != entry.keys.joinToString(", ") ||
                content != entry.content ||
                priorityText != entry.priority.toString() ||
                probability != entry.probability ||
                position != entry.position ||
                selectedCharId != entry.characterId
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃修改？", fontWeight = FontWeight.Bold) },
            text = { Text("当前编辑的修改尚未保存，确定要退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onCancel()
                }) { Text("放弃", color = Color(0xFFFF6B8A), fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("继续编辑", color = Pink) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = WarmWhite,
        topBar = {
            TopAppBar(
                title = { Text("编辑条目", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = {
                        if (hasChanges()) showDiscardDialog = true else onCancel()
                    }) { Text("取消", color = Pink) }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(entry.copy(
                            keys = keys.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            content = content,
                            enabled = enabled,
                            priority = priorityText.toIntOrNull() ?: 100,
                            characterId = selectedCharId,
                            probability = probability,
                            position = position
                        ))
                    }) { Text("保存", color = PinkDark, fontWeight = FontWeight.Bold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // 关键词
            Text("触发关键词（逗号分隔）", style = MaterialTheme.typography.labelMedium, color = PinkDark)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = keys, onValueChange = { keys = it },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("城堡, 魔法, 龙") },
                shape = RoundedCornerShape(16.dp), singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            // 内容
            Text("世界观内容", style = MaterialTheme.typography.labelMedium, color = PinkDark)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8,
                placeholder = { Text("这个世界有魔法和龙...") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(12.dp))

            // 优先级
            Text("优先级（数字越小越优先）", style = MaterialTheme.typography.labelMedium, color = PinkDark)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = priorityText, onValueChange = { priorityText = it.filter { c -> c.isDigit() } },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(16.dp), placeholder = { Text("100") }
            )
            Spacer(Modifier.height(12.dp))

            // 插入位置
            Text("插入位置", style = MaterialTheme.typography.labelMedium, color = PinkDark)
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = positionExpanded,
                onExpandedChange = { positionExpanded = it }
            ) {
                val positionLabel = when (position) {
                    WorldEntryPosition.BEFORE_SYSTEM -> "📌 System Prompt 最前面"
                    WorldEntryPosition.AFTER_SYSTEM -> "📎 System Prompt 最后面"
                    WorldEntryPosition.BEFORE_USER -> "💬 用户消息之前"
                }
                OutlinedTextField(
                    value = positionLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = positionExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(
                        type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = positionExpanded,
                    onDismissRequest = { positionExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📌 System Prompt 最前面") },
                        onClick = { position = WorldEntryPosition.BEFORE_SYSTEM; positionExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("📎 System Prompt 最后面") },
                        onClick = { position = WorldEntryPosition.AFTER_SYSTEM; positionExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("💬 用户消息之前") },
                        onClick = { position = WorldEntryPosition.BEFORE_USER; positionExpanded = false }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 角色绑定
            Text("绑定角色（可选）", style = MaterialTheme.typography.labelMedium, color = PinkDark)
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = charExpanded,
                onExpandedChange = { charExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCharName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = charExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(
                        type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = charExpanded,
                    onDismissRequest = { charExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("🌐 全局（所有角色生效）") },
                        onClick = { selectedCharId = null; charExpanded = false }
                    )
                    characters.forEach { char ->
                        DropdownMenuItem(
                            text = { Text(char.name) },
                            onClick = { selectedCharId = char.id; charExpanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 概率触发
            Text("触发概率: ${(probability * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = PinkDark)
            Spacer(Modifier.height(4.dp))
            Slider(
                value = probability,
                onValueChange = { probability = it },
                valueRange = 0f..1f,
                steps = 19,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0% 永不大", fontSize = 11.sp, color = TextGray)
                Text("100% 必触发", fontSize = 11.sp, color = TextGray)
            }
            Spacer(Modifier.height(12.dp))

            // 启用开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("启用", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
        }
    }
}
