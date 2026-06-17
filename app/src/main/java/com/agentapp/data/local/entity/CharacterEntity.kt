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
    val mesExample: String,
    val systemPrompt: String,
    val creatorNotes: String,
    val postHistoryInstructions: String,
    val alternateGreetings: List<String>,
    val nicknames: List<String>,
    val tags: List<String>,
    val creator: String,
    val characterVersion: String,
    val spec: String,
    val specVersion: String,
    val talkativeness: Float,
    val fav: Boolean,
    val depthPrompt: String,
    val worldName: String,
    val avatarUri: String,
    val worldBookEnabled: Boolean = true,
    val createdAt: Long
)
