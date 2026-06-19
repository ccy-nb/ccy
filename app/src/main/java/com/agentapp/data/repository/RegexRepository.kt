package com.agentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentapp.data.model.RegexScript

private val Context.regexDataStore: DataStore<Preferences> by preferencesDataStore(name = "regex_scripts")

class RegexRepository(context: Context) : ListRepository<RegexScript>(
    store = context.regexDataStore,
    key = stringPreferencesKey("regex_scripts"),
    elementSerializer = RegexScript.serializer()
) {
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
