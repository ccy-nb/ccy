package com.agentapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentapp.data.api.ApiFactory
import com.agentapp.data.api.ConnectionResult
import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Persona
import com.agentapp.data.model.Preset
import com.agentapp.data.model.StLorebook
import com.agentapp.data.model.WorldBook
import com.agentapp.data.model.WorldEntry
import com.agentapp.data.model.WorldEntryPosition
import com.agentapp.data.repository.PersonaRepository
import com.agentapp.data.repository.PresetRepository
import com.agentapp.data.repository.SettingsRepository
import com.agentapp.data.repository.WorldRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    val worldRepo = WorldRepository(application)
    val personaRepo = PersonaRepository(application)
    val presetRepo = PresetRepository(application)
    private val apiFactory = ApiFactory()

    private val _config = MutableStateFlow(ApiConfig())
    val config: StateFlow<ApiConfig> = _config.asStateFlow()

    private val _selectedTheme = MutableStateFlow("system")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private val _testSuccess = MutableStateFlow(false)
    val testSuccess: StateFlow<Boolean> = _testSuccess.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _worldEntryCount = MutableStateFlow(0)
    val worldEntryCount: StateFlow<Int> = _worldEntryCount.asStateFlow()

    private val _persona = MutableStateFlow(Persona())
    val persona: StateFlow<Persona> = _persona.asStateFlow()

    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    init {
        viewModelScope.launch {
            _config.value = settingsRepo.getApiConfigSync()
        }
        viewModelScope.launch {
            settingsRepo.getThemeMode().collect { mode ->
                _selectedTheme.value = mode
            }
        }
        viewModelScope.launch {
            worldRepo.listFlow().collect { entries ->
                _worldEntryCount.value = entries.size
            }
        }
        viewModelScope.launch {
            personaRepo.flow().collect { persona ->
                _persona.value = persona
            }
        }
        viewModelScope.launch {
            presetRepo.flow().collect { presets ->
                _presets.value = presets
            }
        }
    }

    fun updateConfig(transform: (ApiConfig) -> ApiConfig) {
        val oldConfig = _config.value
        val newConfig = transform(oldConfig)
        _config.value = newConfig
        // 仅在 url/provider/apiKey 改变时清空测试状态
        if (oldConfig.baseUrl != newConfig.baseUrl ||
                oldConfig.provider != newConfig.provider ||
                oldConfig.apiKey != newConfig.apiKey) {
            _testResult.value = null
            _testSuccess.value = false
            _availableModels.value = emptyList()
        }
    }

    fun setSelectedTheme(mode: String) {
        _selectedTheme.value = mode
    }

    fun testConnection() {
        if (_testing.value) return
        _testing.value = true
        _testResult.value = null
        _testSuccess.value = false
        _availableModels.value = emptyList()

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                apiFactory.testConnection(_config.value)
            }
            when (result) {
                is ConnectionResult.Success -> {
                    _testResult.value = "✅ 连接成功 (${result.models.size} 个模型)"
                    _testSuccess.value = true
                    _availableModels.value = result.models
                }
                is ConnectionResult.Fail -> {
                    _testResult.value = "❌ ${result.reason}"
                    _testSuccess.value = false
                }
            }
            _testing.value = false
        }
    }

    fun save() {
        viewModelScope.launch {
            settingsRepo.saveApiConfig(_config.value)
            settingsRepo.saveThemeMode(_selectedTheme.value)
            personaRepo.save(_persona.value)
        }
    }

    fun updatePersona(transform: (Persona) -> Persona) {
        _persona.value = transform(_persona.value)
    }

    fun savePreset(preset: Preset) {
        viewModelScope.launch { presetRepo.saveOne(preset) }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepo.delete(id) }
    }

    /** 导出预设为 JSON 并通过分享发送（使用 FileProvider 避免 EXTRA_TEXT 500KB 限制） */
    fun exportPresets(presets: List<Preset>, context: android.content.Context) {
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        val serializer = kotlinx.serialization.builtins.ListSerializer(Preset.serializer())
        val jsonStr = json.encodeToString(serializer, presets)
        // 写入缓存文件，通过 FileProvider 分享
        try {
            val file = java.io.File(context.cacheDir, "agent_app_presets_${System.currentTimeMillis()}.json")
            file.writeText(jsonStr)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Agent App 预设导出")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "导出预设"))
        } catch (e: Exception) {
            // 回退：直接文本分享（小量预设）
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_TEXT, jsonStr)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Agent App 预设导出")
            }
            context.startActivity(android.content.Intent.createChooser(intent, "导出预设"))
        }
    }

    /** 导入预设：从剪贴板读取 JSON 并解析 */
    fun importPreset(context: android.content.Context) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = clipboard?.primaryClip
        val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0)?.text?.toString() else null
        if (text != null && text.isNotBlank()) {
            importPresetFromJson(text)
        }
    }

    /** 导入 SillyTavern 格式的世界书/json */
    fun importWorldBookFromJson(jsonStr: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                // 尝试解析为 ST 的 lorebook 格式（entries 数组在最外层）
                val stLorebook = json.decodeFromString<StLorebook>(jsonStr)
                val book = WorldBook(
                    id = java.util.UUID.randomUUID().toString(),
                    name = stLorebook.name.ifBlank { "导入的世界书" },
                    createdAt = System.currentTimeMillis()
                )
                worldRepo.saveBook(book)

                val entries = stLorebook.entries.map { entry ->
                    WorldEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        worldBookId = book.id,
                        keys = entry.keys,
                        content = entry.content,
                        enabled = entry.enabled,
                        priority = entry.priority,
                        probability = entry.probability,
                        position = when (entry.position) {
                            "before_character_description" -> WorldEntryPosition.BEFORE_SYSTEM
                            "after_character_description" -> WorldEntryPosition.AFTER_SYSTEM
                            "before_user_query" -> WorldEntryPosition.BEFORE_USER
                            "after_user_query" -> WorldEntryPosition.AFTER_USER
                            "before_assistant_response" -> WorldEntryPosition.BEFORE_ASSISTANT
                            "after_assistant_response" -> WorldEntryPosition.AFTER_ASSISTANT
                            else -> WorldEntryPosition.AFTER_SYSTEM
                        },
                        createdAt = System.currentTimeMillis()
                    )
                }
                val saved = worldRepo.saveAll(entries)
                android.widget.Toast.makeText(context, "✅ 导入世界书「${book.name}」($saved 条条目)", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("SettingsVM", "导入世界书失败", e)
                android.widget.Toast.makeText(context, "❌ 导入失败：${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    /** 解析预设 JSON 字符串并保存 */
    fun importPresetFromJson(jsonStr: String) {
        viewModelScope.launch {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val presets = json.decodeFromString<List<Preset>>(jsonStr)
                presets.forEach { presetRepo.saveOne(it) }
            } catch (e: Exception) {
                // 尝试单个预设
                try {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val preset = json.decodeFromString<Preset>(jsonStr)
                    presetRepo.saveOne(preset)
                } catch (e2: Exception) {
                    // 解析失败，静默处理
                    android.util.Log.e("SettingsVM", "导入预设失败", e2)
                }
            }
        }
    }
}
