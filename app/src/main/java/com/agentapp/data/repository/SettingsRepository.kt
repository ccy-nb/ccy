package com.agentapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.ApiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_api_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private val API_CONFIG_KEY = stringPreferencesKey("api_config")
        private val ENCRYPTED_API_KEY = "encrypted_api_key"
        private val DEFAULT_CHARACTER_ID = stringPreferencesKey("default_character")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    fun getApiConfig(): Flow<ApiConfig?> {
        return context.dataStore.data.map { prefs ->
            val str = prefs[API_CONFIG_KEY] ?: return@map null
            try {
                val config = json.decodeFromString<ApiConfig>(str)
                // Restore encrypted apiKey
                val encryptedKey = securePrefs.getString(ENCRYPTED_API_KEY, null)
                if (encryptedKey != null) config.copy(apiKey = encryptedKey) else config
            } catch (_: Exception) { null }
        }
    }

    suspend fun saveApiConfig(config: ApiConfig) {
        // Save apiKey encrypted separately
        securePrefs.edit().putString(ENCRYPTED_API_KEY, config.apiKey).apply()
        // Save rest of config without plaintext apiKey
        val safeConfig = config.copy(apiKey = "")
        context.dataStore.edit { prefs ->
            prefs[API_CONFIG_KEY] = json.encodeToString(ApiConfig.serializer(), safeConfig)
        }
    }

    suspend fun getApiConfigSync(): ApiConfig {
        return getApiConfig().first() ?: ApiConfig()
    }

    suspend fun saveDefaultCharacter(id: String) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_CHARACTER_ID] = id
        }
    }

    suspend fun getDefaultCharacter(): String? {
        return context.dataStore.data.first()[DEFAULT_CHARACTER_ID]
    }

    fun getThemeMode(): Flow<String> {
        return context.dataStore.data.map { it[THEME_MODE_KEY] ?: "system" }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode }
    }
}
