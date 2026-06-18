@file:Suppress("DEPRECATION")

package com.agentapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.ApiProvider
import com.agentapp.data.model.Persona
import com.agentapp.data.model.Preset
import com.agentapp.ui.theme.Pink
import com.agentapp.ui.theme.PinkDark
import com.agentapp.ui.theme.PinkLight
import com.agentapp.ui.theme.TextGray
import com.agentapp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    onNavigateToWorldBook: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val config by settingsViewModel.config.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val testing by settingsViewModel.testing.collectAsState()
    val testResult by settingsViewModel.testResult.collectAsState()
    val testSuccess by settingsViewModel.testSuccess.collectAsState()
    val availableModels by settingsViewModel.availableModels.collectAsState()
    val persona by settingsViewModel.persona.collectAsState()
    val presets by settingsViewModel.presets.collectAsState()
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    var presetEditDialog by remember { mutableStateOf<Preset?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Pink,
        unfocusedBorderColor = Color(0xFFE8DDE8),
        cursorColor = Pink
    )

    // 预设编辑弹窗
    if (presetEditDialog != null) {
        PresetEditDialog(
            preset = presetEditDialog!!,
            onSave = { settingsViewModel.savePreset(it); presetEditDialog = null },
            onDismiss = { presetEditDialog = null }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("⚙️ 设置", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            CollapsibleSection("🔌 API 连接") {
            Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    // Provider
                    ExposedDropdownMenuBox(expanded = providerExpanded, onExpandedChange = { providerExpanded = it }) {
                        val providerLabel = when (config.provider) {
                            ApiProvider.DEEPSEEK -> "DeepSeek"
                            ApiProvider.OPENAI -> "OpenAI"
                            ApiProvider.CLAUDE -> "Claude (Anthropic)"
                            ApiProvider.NVIDIA -> "NVIDIA 英伟达"
                            ApiProvider.GOOGLE -> "Google Gemini"
                            ApiProvider.GROQ -> "Groq"
                            ApiProvider.CUSTOM -> "自定义"
                        }
                        OutlinedTextField(
                            value = providerLabel, onValueChange = {}, readOnly = true,
                            label = { Text("提供商") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(
                                type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, enabled = true
                            ),
                            shape = FieldShape, colors = fieldColors
                        )
                        ExposedDropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                            listOf(
                                Triple("DeepSeek", ApiProvider.DEEPSEEK, "deepseek-chat" to "https://api.deepseek.com/v1"),
                                Triple("OpenAI", ApiProvider.OPENAI, "gpt-4" to "https://api.openai.com/v1"),
                                Triple("Claude (Anthropic)", ApiProvider.CLAUDE, "claude-3-haiku-20240307" to "https://api.anthropic.com/v1"),
                                Triple("NVIDIA 英伟达", ApiProvider.NVIDIA, "meta/llama-3.1-70b-instruct" to "https://integrate.api.nvidia.com/v1"),
                                Triple("Google Gemini", ApiProvider.GOOGLE, "gemini-2.0-flash" to "https://generativelanguage.googleapis.com/v1beta/openai"),
                                Triple("Groq", ApiProvider.GROQ, "llama-3.3-70b-versatile" to "https://api.groq.com/openai/v1"),
                                Triple("自定义 (OpenAI 兼容)", ApiProvider.CUSTOM, "" to "")
                            ).forEach { (label, provider, defaults) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    settingsViewModel.updateConfig {
                                        it.copy(provider = provider, name = if (provider == ApiProvider.CUSTOM) "自定义" else label,
                                            baseUrl = if (defaults.first.isNotEmpty()) defaults.second else it.baseUrl,
                                            model = if (defaults.first.isNotEmpty()) defaults.first else it.model)
                                    }
                                    providerExpanded = false
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // URL + Key 紧凑行
                    OutlinedTextField(
                        value = config.baseUrl, onValueChange = { v -> settingsViewModel.updateConfig { it.copy(baseUrl = v) } },
                        label = { Text("API 地址") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.apiKey, onValueChange = { v -> settingsViewModel.updateConfig { it.copy(apiKey = v) } },
                        label = { Text("API Key") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { showKey = !showKey }) {
                                Text(if (showKey) "隐藏" else "显示", color = Pink, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))

                    // 模型选择
                    if (testSuccess && availableModels.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
                            OutlinedTextField(
                                value = config.model, onValueChange = {}, readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(
                                    type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, enabled = true
                                ),
                                shape = FieldShape, colors = fieldColors, label = { Text("模型") }
                            )
                            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(text = { Text(model) }, onClick = {
                                        settingsViewModel.updateConfig { it.copy(model = model) }
                                        modelExpanded = false
                                    })
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = config.model, onValueChange = { v -> settingsViewModel.updateConfig { it.copy(model = v) } },
                            label = { Text("模型") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Temperature + MaxTokens 紧凑行
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = config.temperature.toString(),
                            onValueChange = { v -> v.toFloatOrNull()?.let { f -> settingsViewModel.updateConfig { cfg -> cfg.copy(temperature = f.coerceIn(0f, 2f)) } } },
                            label = { Text("Temperature") }, singleLine = true,
                            modifier = Modifier.weight(1f), shape = FieldShape, colors = fieldColors
                        )
                        OutlinedTextField(
                            value = if (config.maxTokens == 0) "" else config.maxTokens.toString(),
                            onValueChange = { v -> settingsViewModel.updateConfig { it.copy(maxTokens = if (v.isBlank()) 0 else (v.toIntOrNull() ?: it.maxTokens)) } },
                            label = { Text("最大 Token") }, singleLine = true,
                            modifier = Modifier.weight(1f), shape = FieldShape, colors = fieldColors
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // 测试连接
                    Button(
                        onClick = { settingsViewModel.testConnection() }, enabled = !testing,
                        modifier = Modifier.fillMaxWidth().height(44.dp), shape = FieldShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (testSuccess) Color(0xFF4CAF50) else PinkDark,
                            disabledContainerColor = Color(0xFFE0E0E0)
                        )
                    ) {
                        if (testing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (testing) "测试中..." else if (testSuccess) "✅ 已连接" else "🔌 测试连接",
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (testResult != null && !testSuccess) {
                        Text(testResult!!, modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFE86A8A))
                    }
                }
            }

            }

            CollapsibleSection("👤 我的身份") {
            Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("设定你在 AI 眼中的形象，会注入到每次对话中", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = persona.name, onValueChange = { v -> settingsViewModel.updatePersona { p -> p.copy(name = v) } },
                        label = { Text("你的名字") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = persona.description, onValueChange = { v -> settingsViewModel.updatePersona { p -> p.copy(description = v) } },
                        label = { Text("你的描述（外貌、性格等）") }, minLines = 2, maxLines = 3,
                        modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = persona.personality, onValueChange = { v -> settingsViewModel.updatePersona { p -> p.copy(personality = v) } },
                        label = { Text("你的性格") }, minLines = 2, maxLines = 3,
                        modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors
                    )
                }
            }

            }

            CollapsibleSection(
                title = "📋 推理预设",
                actions = {
                    TextButton(onClick = { presetEditDialog = Preset() }) {
                        Text("＋ 新建", color = Pink, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { settingsViewModel.exportPresets(presets, context) }) {
                        Text("📤", fontSize = 14.sp)
                    }
                    TextButton(onClick = { settingsViewModel.importPreset(context) }) {
                        Text("📥", fontSize = 14.sp)
                    }
                }
            ) {
                TextButton(onClick = { presetEditDialog = Preset() }) {
                    Text("＋ 新建", color = Pink, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = {
                    settingsViewModel.exportPresets(presets, context)
                }) {
                    Text("📤", fontSize = 14.sp)
                }
                TextButton(onClick = {
                    settingsViewModel.importPreset(context)
                }) {
                    Text("📥", fontSize = 14.sp)
                }
            }
            if (presets.isEmpty()) {
                Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 32.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("暂无预设", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Text("预设可以保存不同的 Temperature、模型等参数组合", fontSize = 12.sp, color = TextGray)
                    }
                }
            } else {
                presets.forEach { preset ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(preset.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                if (preset.model.isNotBlank()) {
                                    Text("模型: ${preset.model}", fontSize = 12.sp, color = TextGray)
                                }
                                Text("T:${preset.temperature}  Tok:${preset.maxTokens}  Ctx:${preset.maxContext}  TopP:${preset.topP}",
                                    fontSize = 11.sp, color = TextGray)
                            }
                            IconButton(onClick = { presetEditDialog = preset }) {
                                Text("✏️", fontSize = 14.sp)
                            }
                            IconButton(onClick = { settingsViewModel.deletePreset(preset.id) }) {
                                Text("🗑️", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            CollapsibleSection("🎨 主题") {
            Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("system" to "跟随系统", "light" to "可爱粉", "dark" to "暗色").forEach { (mode, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = selectedTheme == mode,
                            onClick = { settingsViewModel.setSelectedTheme(mode) },
                            label = { Text(label, fontSize = 13.sp) },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkLight, selectedLabelColor = PinkDark
                            )
                        )
                    }
                }
            }

            }

            CollapsibleSection("📖 世界书") {
            Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("从 SillyTavern 格式的 lorebook JSON 导入", fontSize = 12.sp, color = TextGray)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = clipboard?.primaryClip
                            val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0)?.text?.toString() else null
                            if (text != null && text.isNotBlank()) {
                                settingsViewModel.importWorldBookFromJson(text, context)
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("剪贴板为空，请先复制 JSON") }
                            }
                        }) {
                            Text("📥 从剪贴板导入", color = Pink, fontSize = 13.sp)
                        }
                        TextButton(onClick = onNavigateToWorldBook) {
                            Text("📂 管理世界书", color = Pink, fontSize = 13.sp)
                        }
                    }
                }
            }
            }

            Spacer(Modifier.height(16.dp))

            // ===== 保存 =====
            Button(
                onClick = {
                    settingsViewModel.save()
                    scope.launch { snackbarHostState.showSnackbar("配置已保存 ✓") }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = FieldShape,
                colors = ButtonDefaults.buttonColors(containerColor = PinkDark)
            ) { Text("💾 保存配置", fontWeight = FontWeight.Bold, color = Color.White) }

            Spacer(Modifier.height(8.dp))
            Text("🔒 所有数据仅保存在本地", style = MaterialTheme.typography.bodySmall, color = TextGray,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, fontWeight = FontWeight.Bold, color = PinkDark, fontSize = 14.sp,
        modifier = modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

/** 可折叠设置分区 — 点标题展开/收起 */
@Composable
private fun CollapsibleSection(
    title: String,
    initialExpanded: Boolean = true,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (expanded) "▼" else "▶",
                fontSize = 10.sp,
                color = PinkDark,
                modifier = Modifier.padding(end = 6.dp)
            )
            SectionTitle(title, modifier = Modifier.weight(1f))
            if (actions != null) actions()
        }
        AnimatedVisibility(visible = expanded) {
            content()
        }
    }
}

// === 预设编辑弹窗 ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetEditDialog(
    preset: Preset,
    onSave: (Preset) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(preset.name) }
    var temperature by remember { mutableStateOf(preset.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(preset.maxTokens.toString()) }
    var maxContext by remember { mutableStateOf(preset.maxContext.toString()) }
    var topP by remember { mutableStateOf(preset.topP.toString()) }
    var topK by remember { mutableStateOf(if (preset.topK == 0) "" else preset.topK.toString()) }
    var frequencyPenalty by remember { mutableStateOf(preset.frequencyPenalty.toString()) }
    var presencePenalty by remember { mutableStateOf(preset.presencePenalty.toString()) }
    var minP by remember { mutableStateOf(preset.minP.toString()) }
    var model by remember { mutableStateOf(preset.model) }
    var systemPrompt by remember { mutableStateOf(preset.systemPrompt) }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pink, unfocusedBorderColor = Color(0xFFE8DDE8), cursorColor = Pink)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset.id.isEmpty()) "新建预设 📋" else "编辑预设 ✏️", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("预设名称") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型（留空=全局）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(6.dp))

                // 第 1 行：Temperature + MaxTokens + MaxContext
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = temperature, onValueChange = { temperature = it },
                        label = { Text("Temp") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    OutlinedTextField(value = maxTokens, onValueChange = { maxTokens = it },
                        label = { Text("MaxTok") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    OutlinedTextField(value = maxContext, onValueChange = { maxContext = it },
                        label = { Text("Ctx") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                }
                Spacer(Modifier.height(6.dp))

                // 第 2 行：TopP + TopK + MinP
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = topP, onValueChange = { topP = it },
                        label = { Text("TopP") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    OutlinedTextField(value = topK, onValueChange = { topK = it },
                        label = { Text("TopK") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    OutlinedTextField(value = minP, onValueChange = { minP = it },
                        label = { Text("MinP") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                }
                Spacer(Modifier.height(6.dp))

                // 第 3 行：FreqPenalty + PresencePenalty
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = frequencyPenalty, onValueChange = { frequencyPenalty = it },
                        label = { Text("FreqPen") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    OutlinedTextField(value = presencePenalty, onValueChange = { presencePenalty = it },
                        label = { Text("PresPen") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                }
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(value = systemPrompt, onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt（可选）") }, minLines = 2, maxLines = 3,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val temp = temperature.toFloatOrNull()?.coerceIn(0f, 2f) ?: 0.7f
                val tokens = maxTokens.toIntOrNull() ?: 300
                val ctx = maxContext.toIntOrNull() ?: 4096
                val tp = topP.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1.0f
                val tk = topK.toIntOrNull()?.coerceIn(0, 1000) ?: 0
                val fp = frequencyPenalty.toFloatOrNull()?.coerceIn(-2f, 2f) ?: 0f
                val pp = presencePenalty.toFloatOrNull()?.coerceIn(-2f, 2f) ?: 0f
                val mp = minP.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f
                onSave(preset.copy(
                    name = name,
                    temperature = temp,
                    maxTokens = tokens,
                    maxContext = ctx,
                    topP = tp,
                    topK = tk,
                    frequencyPenalty = fp,
                    presencePenalty = pp,
                    minP = mp,
                    model = model,
                    systemPrompt = systemPrompt
                ))
            }) { Text("保存", fontWeight = FontWeight.Bold, color = PinkDark) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = CardShape
    )
}
