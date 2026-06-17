package com.agentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Character(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    @kotlinx.serialization.SerialName("first_mes")
    val greeting: String = "",
    val avatarUri: String = "",
    @kotlinx.serialization.SerialName("system_prompt")
    val systemPrompt: String = "",
    val worldBookEnabled: Boolean = true,   // 该角色是否启用世界书
    val createdAt: Long = System.currentTimeMillis()
) {
    fun buildSystemPrompt(): String {
        if (systemPrompt.isNotBlank()) return systemPrompt
        val sb = StringBuilder()
        sb.appendLine("你是 $name。")
        if (description.isNotBlank()) { sb.appendLine(); sb.appendLine("描述: $description") }
        if (personality.isNotBlank()) { sb.appendLine(); sb.appendLine("性格: $personality") }
        if (scenario.isNotBlank()) { sb.appendLine(); sb.appendLine("场景: $scenario") }
        sb.appendLine()
        sb.appendLine("请以 $name 的身份回复，保持角色设定。")
        return sb.toString()
    }
}
