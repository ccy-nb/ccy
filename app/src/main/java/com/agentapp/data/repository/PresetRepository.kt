package com.agentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentapp.data.model.Preset

private val Context.presetDataStore: DataStore<Preferences> by preferencesDataStore(name = "presets")

class PresetRepository(context: Context) : ListRepository<Preset>(
    store = context.presetDataStore,
    key = stringPreferencesKey("presets"),
    elementSerializer = Preset.serializer()
) {
    suspend fun saveOne(preset: Preset) = saveOne(preset) { it.id }
    suspend fun delete(id: String) = delete(id) { it.id }
}
