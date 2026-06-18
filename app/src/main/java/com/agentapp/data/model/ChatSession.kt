package com.agentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val characterId: String,
    val messages: List<Message> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val parentSessionId: String? = null  // 分支来源会话 ID
)
