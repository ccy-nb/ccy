package com.agentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_entries")
data class WorldEntryEntity(
    @PrimaryKey val id: String,
    val keys: List<String> = emptyList(),  // JSON 数组存储，TypeConverter 自动转换
    val content: String,
    val enabled: Boolean,
    val priority: Int = 100,        // 1=最高优先级
    val characterId: String? = null,// null=全局
    val probability: Float = 1.0f,  // 0.0~1.0
    val position: String = "AFTER_SYSTEM", // BEFORE_SYSTEM / AFTER_SYSTEM / BEFORE_USER
    val createdAt: Long
)
