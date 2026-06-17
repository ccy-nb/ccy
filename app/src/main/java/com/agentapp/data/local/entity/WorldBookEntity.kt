package com.agentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_books")
data class WorldBookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val characterId: String? = null,
    val createdAt: Long
)
