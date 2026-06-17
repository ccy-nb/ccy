package com.agentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentapp.data.model.RegexScript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.regexDataStore: DataStore<Preferences> by preferencesDataStore(name = "regex_scripts")

class RegexRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val SCRIPTS_KEY = stringPreferencesKey("regex_scripts")
    }

    fun listFlow(): Flow<List<RegexScript>> {
        return context.regexDataStore.data.map { prefs ->
            val str = prefs[SCRIPTS_KEY] ?: return@map emptyList()
            try { json.decodeFromString<List<RegexScript>>(str) } catch (_: Exception) { emptyList() }
        }
    }

    suspend fun list(): List<RegexScript> {
        return listFlow().first()
    }

    suspend fun saveAll(scripts: List<RegexScript>) {
        context.regexDataStore.edit { prefs ->
            prefs[SCRIPTS_KEY] = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(RegexScript.serializer()),
                scripts
            )
        }
    }

    /** 对文本应用所有启用的正则脚本，返回替换后的文本 */
    fun applyScripts(text: String, scripts: List<RegexScript>): String {
        var result = text
        scripts
            .filter { it.enabled }
            .sortedBy { it.priority }
            .forEach { script ->
                try {
                    val regex = Regex(script.findRegex, RegexOption.DOT_MATCHES_ALL)
                    result = regex.replace(result, script.replaceString)
                } catch (_: Exception) { /* 跳过无效正则 */ }
            }
        return result
    }
}
