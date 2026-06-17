package com.agentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val greeting: String,
    val avatarUri: String,
    val systemPrompt: String,
    val worldBookEnabled: Boolean = true,
    val createdAt: Long
)
