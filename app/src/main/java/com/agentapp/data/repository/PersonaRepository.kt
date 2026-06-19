package com.agentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentapp.data.model.Persona

private val Context.personaDataStore: DataStore<Preferences> by preferencesDataStore(name = "persona")

class PersonaRepository(context: Context) : SingleValueRepository<Persona>(
    store = context.personaDataStore,
    key = stringPreferencesKey("persona"),
    serializer = Persona.serializer(),
    default = Persona()
)
