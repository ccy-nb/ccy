@file:Suppress("DEPRECATION")
package com.agentapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.agentapp.ui.theme.PinkDark
import com.agentapp.ui.theme.TextGray

private val CardShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(16.dp)

// ========= API 设置内容 =========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsApiContent(
    config: ApiConfig,
    testing: Boolean,
    testResult: String?,
    testSuccess: Boolean,
    availableModels: List<String>,
    providerExpanded: Boolean,
    modelExpanded: Boolean,
    showKey: Boolean,
    onProviderExpandedChange: (Boolean) -> Unit,
    onModelExpandedChange: (Boolean) -> Unit,
    onShowKeyChange: (Boolean) -> Unit,
    onUpdateConfig: ((ApiConfig) -> ApiConfig) -> Unit,
    onTestConnection: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Card(shape = CardShape, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            // Provider
            ExposedDropdownMenuBox(expanded = providerExpanded, onExpandedChange = onProviderExpandedChange) {
                val providerLabel = when (config.provider) {
                    ApiProvider.DEEPSEEK -> "DeepSeek"
                    ApiProvider.OPENAI -> "OpenAI"
                    ApiProvider.CLAUDE -> "Claude (Anthropic)"
                    ApiProvider.NVIDIA -> "NVIDIA 英伟达"
                    ApiProvider.GOOGLE -> "Google Gemini"
                    ApiProvider.GROQ -> "Groq"
                    ApiProvider.CUSTOM -> "自定义"
                }
                OutlinedTextField(value = providerLabel, onValueChange = {}, readOnly = true,
                    label = { Text("提供商") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, enabled = true),
                    shape = FieldShape, colors = fieldColors)
                ExposedDropdownMenu(expanded = providerExpanded, onDismissRequest = { onProviderExpandedChange(false) }) {
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
                            onUpdateConfig { it.copy(provider = provider, name = if (provider == ApiProvider.CUSTOM) "自定义" else label,
                                baseUrl = if (defaults.first.isNotEmpty()) defaults.second else it.baseUrl,
                                model = if (defaults.first.isNotEmpty()) defaults.first else it.model) }
                            onProviderExpandedChange(false)
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 显示名称
            OutlinedTextField(value = config.name, onValueChange = { v -> onUpdateConfig { c -> c.copy(name = v) } },
                label = { Text("显示名称") }, modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors)
            Spacer(Modifier.height(8.dp))

            // API 地址
            OutlinedTextField(value = config.baseUrl, onValueChange = { v -> onUpdateConfig { c -> c.copy(baseUrl = v) } },
                label = { Text("API 地址") }, modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors)
            Spacer(Modifier.height(8.dp))

            // API Key
            OutlinedTextField(value = config.apiKey, onValueChange = { v -> onUpdateConfig { c -> c.copy(apiKey = v) } },
                label = { Text("API Key") },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { onShowKeyChange(!showKey) }) {
                        Text(if (showKey) "隐藏" else "显示", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            Spacer(Modifier.height(8.dp))

            // 模型选择
            ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = onModelExpandedChange) {
                OutlinedTextField(value = config.model, onValueChange = { v -> onUpdateConfig { c -> c.copy(model = v) } },
                    label = { Text("模型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, enabled = true),
                    shape = FieldShape, colors = fieldColors)
                if (availableModels.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { onModelExpandedChange(false) }) {
                        availableModels.forEach { model ->
                            DropdownMenuItem(text = { Text(model) }, onClick = {
                                onUpdateConfig { it.copy(model = model) }
                                onModelExpandedChange(false)
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 测试连接按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onTestConnection,
                    enabled = !testing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (testSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (testing) "测试中..." else "测试连接")
                }
                if (testResult != null) {
                    Spacer(Modifier.width(12.dp))
                    Text(testResult, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ========= 身份设置内容 =========

@Composable
fun SettingsPersonaContent(persona: Persona, onSave: (Persona) -> Unit) {
    var name by remember(persona) { mutableStateOf(persona.name) }
    var description by remember(persona) { mutableStateOf(persona.description) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Card(shape = CardShape, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("我的名字") }, modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("自我介绍") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = FieldShape, colors = fieldColors)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSave(persona.copy(name = name, description = description)) },
                shape = RoundedCornerShape(14.dp)) {
                Text("保存身份")
            }
        }
    }
}

// ========= 预设设置内容 =========

@Composable
fun SettingsPresetContent(presets: List<Preset>, onEdit: (Preset) -> Unit, onDelete: (String) -> Unit) {
    if (presets.isEmpty()) {
        Card(shape = CardShape, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无预设", fontSize = 14.sp, color = TextGray)
                Spacer(Modifier.height(4.dp))
                Text("点击右上角 ＋ 新建", fontSize = 12.sp, color = TextGray)
            }
        }
    } else {
        presets.forEach { preset ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onEdit(preset) }) {
                        Text(preset.name.ifEmpty { "未命名" }, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("T=${preset.temperature} M=${preset.maxTokens}", fontSize = 11.sp, color = TextGray)
                    }
                    TextButton(onClick = { onEdit(preset) }) { Text("编辑", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
                    TextButton(onClick = { onDelete(preset.id) }) { Text("删除", color = PinkDark, fontSize = 13.sp) }
                }
            }
        }
    }
}

// ========= 主题设置内容 =========

@Composable
fun SettingsThemeContent(selectedTheme: String, onSelect: (String) -> Unit) {
    val themes = listOf(
        "system" to "跟随系统",
        "dark" to "深色模式",
        "light" to "浅色模式"
    )

    Card(shape = CardShape, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            themes.forEach { (value, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSelect(value) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedTheme == value, onClick = { onSelect(value) })
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 15.sp)
                }
            }
        }
    }
}

// ========= 预设编辑弹窗 =========

@Composable
fun PresetEditDialog(
    preset: Preset,
    onSave: (Preset) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(preset) { mutableStateOf(preset.name) }
    var temperature by remember(preset) { mutableStateOf(preset.temperature.toString()) }
    var maxTokens by remember(preset) { mutableStateOf(preset.maxTokens.toString()) }
    var maxContext by remember(preset) { mutableStateOf(preset.maxContext.toString()) }
    var topP by remember(preset) { mutableStateOf(preset.topP.toString()) }
    var topK by remember(preset) { mutableStateOf(preset.topK.toString()) }
    var systemPrompt by remember(preset) { mutableStateOf(preset.systemPrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑预设", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text("Temperature") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = maxTokens, onValueChange = { maxTokens = it }, label = { Text("Max Tokens") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = maxContext, onValueChange = { maxContext = it }, label = { Text("Max Context") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = topP, onValueChange = { topP = it }, label = { Text("Top P") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = topK, onValueChange = { topK = it }, label = { Text("Top K") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = systemPrompt, onValueChange = { systemPrompt = it }, label = { Text("System Prompt") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(14.dp), colors = fieldColors)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = preset.copy(
                    name = name,
                    temperature = temperature.toFloatOrNull() ?: preset.temperature,
                    maxTokens = maxTokens.toIntOrNull() ?: preset.maxTokens,
                    maxContext = maxContext.toIntOrNull() ?: preset.maxContext,
                    topP = topP.toFloatOrNull() ?: preset.topP,
                    topK = topK.toIntOrNull() ?: preset.topK,
                    systemPrompt = systemPrompt
                )
                onSave(updated)
            }) { Text("保存", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
