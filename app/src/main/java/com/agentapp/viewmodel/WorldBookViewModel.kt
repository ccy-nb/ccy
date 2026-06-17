package com.agentapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentapp.data.model.Character
import com.agentapp.data.model.WorldEntry
import com.agentapp.data.repository.CharacterRepository
import com.agentapp.data.repository.WorldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorldBookViewModel(application: Application) : AndroidViewModel(application) {
    private val worldRepo = WorldRepository(application)
    private val characterRepo = CharacterRepository(application)

    private val _entries = MutableStateFlow<List<WorldEntry>>(emptyList())
    val entries: StateFlow<List<WorldEntry>> = _entries.asStateFlow()

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    init {
        viewModelScope.launch {
            worldRepo.listFlow().collect { list ->
                _entries.value = list
            }
        }
        viewModelScope.launch {
            characterRepo.listFlow().collect { list ->
                _characters.value = list
            }
        }
    }

    fun saveEntry(entry: WorldEntry) {
        viewModelScope.launch {
            worldRepo.save(entry)
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            worldRepo.delete(id)
        }
    }
}
