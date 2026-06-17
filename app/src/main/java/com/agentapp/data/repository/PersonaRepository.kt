package com.agentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentapp.data.model.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.personaDataStore: DataStore<Preferences> by preferencesDataStore(name = "persona")

class PersonaRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val PERSONA_KEY = stringPreferencesKey("persona")
    }

    fun getFlow(): Flow<Persona> {
        return context.personaDataStore.data.map { prefs ->
            val str = prefs[PERSONA_KEY] ?: return@map Persona()
            try { json.decodeFromString<Persona>(str) } catch (_: Exception) { Persona() }
        }
    }

    suspend fun get(): Persona {
        return getFlow().first()
    }

    suspend fun save(persona: Persona) {
        context.personaDataStore.edit { prefs ->
            prefs[PERSONA_KEY] = json.encodeToString(Persona.serializer(), persona)
        }
    }
}
