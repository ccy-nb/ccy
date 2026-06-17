package com.agentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentapp.data.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.presetDataStore: DataStore<Preferences> by preferencesDataStore(name = "presets")

class PresetRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val PRESETS_KEY = stringPreferencesKey("presets")
    }

    fun listFlow(): Flow<List<Preset>> {
        return context.presetDataStore.data.map { prefs ->
            val str = prefs[PRESETS_KEY] ?: return@map emptyList()
            try { json.decodeFromString<List<Preset>>(str) } catch (_: Exception) { emptyList() }
        }
    }

    suspend fun list(): List<Preset> {
        return listFlow().first()
    }

    suspend fun save(presets: List<Preset>) {
        context.presetDataStore.edit { prefs ->
            prefs[PRESETS_KEY] = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(Preset.serializer()),
                presets
            )
        }
    }

    suspend fun saveOne(preset: Preset) {
        val list = list().toMutableList()
        val idx = list.indexOfFirst { it.id == preset.id }
        if (idx >= 0) list[idx] = preset else list.add(preset)
        save(list)
    }

    suspend fun delete(id: String) {
        save(list().filter { it.id != id })
    }
}
