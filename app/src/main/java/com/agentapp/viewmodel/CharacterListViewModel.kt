package com.agentapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentapp.data.model.Character
import com.agentapp.data.repository.CharacterRepository
import com.agentapp.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CharacterListViewModel(application: Application) : AndroidViewModel(application) {
    private val characterRepo = CharacterRepository(application)
    private val chatRepo = ChatRepository(application)

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            characterRepo.listFlow().collect { list ->
                _characters.value = list
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            _characters.value = characterRepo.list()
        }
    }

    fun saveCharacter(character: Character) {
        viewModelScope.launch {
            characterRepo.save(character)
            refresh()
        }
    }

    fun deleteCharacter(id: String) {
        viewModelScope.launch {
            characterRepo.delete(id)
            refresh()
        }
    }

    suspend fun importFromFile(file: java.io.File): Boolean {
        val char = characterRepo.importFromFile(file) ?: return false
        characterRepo.save(char)
        refresh()
        return true
    }

    suspend fun importFromJsonContent(content: String): Boolean {
        val char = characterRepo.importFromJson(content) ?: return false
        characterRepo.save(char)
        refresh()
        return true
    }

    fun startChatWith(characterId: String, onSessionCreated: (String, String) -> Unit) {
        viewModelScope.launch {
            val char = characterRepo.get(characterId) ?: return@launch
            val sessions = chatRepo.list(char.id)
            val session = if (sessions.isNotEmpty()) sessions.first()
            else {
                val ns = chatRepo.create(char.id)
                if (char.greeting.isNotBlank()) {
                    chatRepo.addMessage(
                        ns.id,
                        com.agentapp.data.model.Message(
                            role = com.agentapp.data.model.Role.ASSISTANT,
                            content = char.greeting
                        )
                    )
                }
                chatRepo.get(ns.id) ?: ns
            }
            withContext(Dispatchers.Main) {
                onSessionCreated(session.id, char.name)
            }
        }
    }
}
