package com.agentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorldEntryPosition {
    BEFORE_SYSTEM,   // system prompt 最前面
    AFTER_SYSTEM,    // system prompt 最后面（默认）
    BEFORE_USER      // 用户最后一条消息之前
}

@Serializable
data class WorldEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val keys: List<String> = emptyList(),           // 触发关键词
    val content: String = "",                        // 插入的内容
    val enabled: Boolean = true,
    val priority: Int = 100,                         // 1 = 最高优先级，升序排列
    val characterId: String? = null,                 // null = 全局生效，非空 = 绑定到指定角色
    val probability: Float = 1.0f,                   // 0.0~1.0，触发概率，1.0 = 必然触发
    val position: WorldEntryPosition = WorldEntryPosition.AFTER_SYSTEM, // 插入位置
    val createdAt: Long = System.currentTimeMillis()
)
