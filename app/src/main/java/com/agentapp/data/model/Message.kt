package com.agentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val swipes: List<String> = emptyList(),       // 所有回复版本（AI 消息），第一项=当前
    val currentSwipeId: Int = 0                    // 当前显示第几个版本
)

@Serializable
enum class Role {
    USER, ASSISTANT, SYSTEM
}
