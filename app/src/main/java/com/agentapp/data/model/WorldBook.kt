package com.agentapp.data.model

import kotlinx.serialization.Serializable

/**
 * 世界书 —— 一个角色卡绑定一个世界书，内含多条 WorldEntry。
 * 参考 SillyTavern character_book 设计。
 */
@Serializable
data class WorldBook(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",          // 世界书名称（默认 = 角色名）
    val characterId: String? = null, // 绑定角色，null = 独立世界书
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * SillyTavern 格式的 lorebook 数据类，用于导入兼容。
 * ST 的 character_book 或 world_book JSON 结构。
 */
@Serializable
data class StLorebook(
    val name: String = "",
    val description: String = "",
    val scan_always: Boolean = true,
    val entries: List<StLoreEntry> = emptyList()
)

@Serializable
data class StLoreEntry(
    val keys: List<String> = emptyList(),
    val content: String = "",
    val enabled: Boolean = true,
    val priority: Int = 100,
    val probability: Float = 1.0f,
    val position: String = "after_character_description"  // ST 位置名
)
