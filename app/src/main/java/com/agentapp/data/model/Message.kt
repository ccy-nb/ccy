package com.agentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class Role {
    USER, ASSISTANT, SYSTEM
}
