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
    private var charWatcherJob: Job? = null

    init {
        startCollectors()
    }

    private fun startCollectors() {
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch {
            characterRepo.listFlow().collect { characters ->
                // 取消旧的 per-character collectors，启动新的
                charWatcherJob?.cancel()
                charWatcherJob = Job()
                characters.forEach { char ->
                    launch(charWatcherJob!!) {
                        chatRepo.listFlow(char.id).collect {
                            refresh()
                        }
                    }
                }
                refresh()
            }
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

    fun getSessionsForCharacter(characterId: String): kotlinx.coroutines.flow.Flow<List<ChatSession>> {
        return chatRepo.listFlow(characterId)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepo.delete(sessionId)
        }
    }
}
