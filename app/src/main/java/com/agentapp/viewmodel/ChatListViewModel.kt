package com.agentapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentapp.data.model.ChatSession
import com.agentapp.data.repository.CharacterRepository
import com.agentapp.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {
    private val chatRepo = ChatRepository(application)
    private val characterRepo = CharacterRepository(application)

    private val _sessions = MutableStateFlow<List<Pair<ChatSession, String>>>(emptyList())
    val sessions: StateFlow<List<Pair<ChatSession, String>>> = _sessions.asStateFlow()

    private var collectorJob: Job? = null

    init {
        startCollectors()
    }

    private fun startCollectors() {
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch {
            val characters = characterRepo.list()
            val jobs = characters.map { char ->
                launch {
                    chatRepo.listFlow(char.id).collect { sessions ->
                        refresh()
                    }
                }
            }
            refresh()
            // Keep job references so they can be cancelled via collectorJob children
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val characters = characterRepo.list()
            _sessions.value = characters.flatMap { char ->
                chatRepo.list(char.id).map { it to char.name }
            }.sortedByDescending { it.first.updatedAt }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepo.delete(sessionId)
        }
    }
}
