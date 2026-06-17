package com.agentapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentapp.data.api.ApiFactory
import com.agentapp.data.api.ConnectionResult
import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Persona
import com.agentapp.data.model.Preset
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
            personaRepo.getFlow().collect { persona ->
                _persona.value = persona
            }
        }
        viewModelScope.launch {
            presetRepo.listFlow().collect { presets ->
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
}
