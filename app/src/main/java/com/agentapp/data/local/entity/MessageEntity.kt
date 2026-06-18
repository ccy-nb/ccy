package com.agentapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,   // "USER", "ASSISTANT", "SYSTEM"
    val content: String,
    val timestamp: Long,
    val parentMessageId: String? = null,  // null = 普通消息, 非 null = 同消息的不同 swipe 版本
    val siblingIndex: Int = 0              // 同父消息下的版本序号
)
