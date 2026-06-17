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
