package com.agentapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterId")]
)
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val parentSessionId: String? = null  // 分支来源会话 ID
)
