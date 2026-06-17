package com.agentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_entries")
data class WorldEntryEntity(
    @PrimaryKey val id: String,
    val worldBookId: String? = null, // 所属世界书
    val keys: List<String> = emptyList(),
    val content: String,
    val enabled: Boolean,
    val priority: Int = 100,
    val characterId: String? = null,
    val probability: Float = 1.0f,
    val position: String = "AFTER_SYSTEM",
    val createdAt: Long
)
