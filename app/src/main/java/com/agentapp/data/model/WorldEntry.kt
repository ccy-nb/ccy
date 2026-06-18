package com.agentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorldEntryPosition {
    BEFORE_SYSTEM,   // system prompt 最前面
    AFTER_SYSTEM,    // system prompt 最后面（默认）
    BEFORE_USER,     // 用户最后一条消息之前
    AFTER_USER,      // 用户最后一条消息之后
    BEFORE_ASSISTANT,// AI 回复之前
    AFTER_ASSISTANT  // AI 回复之后
}

@Serializable
data class WorldEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val worldBookId: String? = null,                 // 所属世界书
    val keys: List<String> = emptyList(),
    val content: String = "",
    val enabled: Boolean = true,
    val priority: Int = 100,
    val characterId: String? = null,
    val probability: Float = 1.0f,
    val position: WorldEntryPosition = WorldEntryPosition.AFTER_SYSTEM,
    val createdAt: Long = System.currentTimeMillis()
)
