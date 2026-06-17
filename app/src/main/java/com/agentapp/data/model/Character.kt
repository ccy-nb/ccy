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
    @kotlinx.serialization.SerialName("mes_example")
    val mesExample: String = "",
    @kotlinx.serialization.SerialName("system_prompt")
    val systemPrompt: String = "",
    @kotlinx.serialization.SerialName("creator_notes")
    val creatorNotes: String = "",
    @kotlinx.serialization.SerialName("post_history_instructions")
    val postHistoryInstructions: String = "",
    @kotlinx.serialization.SerialName("alternate_greetings")
    val alternateGreetings: List<String> = emptyList(),
    val nicknames: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val creator: String = "",
    @kotlinx.serialization.SerialName("character_version")
    val characterVersion: String = "",
    val spec: String = "",
    @kotlinx.serialization.SerialName("spec_version")
    val specVersion: String = "",
    val talkativeness: Float = 0.5f,
    val fav: Boolean = false,
    val depthPrompt: String = "",
    @kotlinx.serialization.SerialName("world")
    val worldName: String = "",
    val avatarUri: String = "",
    val worldBookEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 构建角色定义部分（不含 depthPrompt——它单独注入） */
    fun buildCharacterPrompt(): String {
        val sb = StringBuilder()
        sb.appendLine("你是 $name。")
        if (description.isNotBlank()) { sb.appendLine(); sb.appendLine("描述: $description") }
        if (personality.isNotBlank()) { sb.appendLine(); sb.appendLine("性格: $personality") }
        if (scenario.isNotBlank()) { sb.appendLine(); sb.appendLine("场景: $scenario") }
        if (systemPrompt.isNotBlank()) { sb.appendLine(); sb.appendLine("=== 系统指令 ==="); sb.appendLine(systemPrompt) }
        if (mesExample.isNotBlank()) { sb.appendLine(); sb.appendLine("对话示例: $mesExample") }
        if (postHistoryInstructions.isNotBlank()) { sb.appendLine(); sb.appendLine("历史指令: $postHistoryInstructions") }
        if (creatorNotes.isNotBlank()) { sb.appendLine(); sb.appendLine("作者备注: $creatorNotes") }
        sb.appendLine()
        sb.appendLine("请以 $name 的身份回复，保持角色设定。")
        return sb.toString()
    }

    fun buildSystemPrompt(): String {
        val sb = StringBuilder()
        sb.append(buildCharacterPrompt())
        if (depthPrompt.isNotBlank()) { sb.appendLine(); sb.appendLine("=== 深度指令 ==="); sb.appendLine(depthPrompt) }
        return sb.toString()
    }
}
