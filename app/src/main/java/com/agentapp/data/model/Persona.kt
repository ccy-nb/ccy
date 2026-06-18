package com.agentapp.data.model

import kotlinx.serialization.Serializable

/**
 * 用户身份（Persona）—— 定义"我是谁"，在聊天中作为 user 的背景信息注入 system prompt。
 * 参考 SillyTavern 的角色卡设计。
 */
@Serializable
data class Persona(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val personality: String = "",       // 性格
    val avatarUri: String = ""          // 头像
) {
    fun buildPrompt(): String {
        if (name.isBlank() && description.isBlank() && personality.isBlank()) return ""
        val sb = StringBuilder()
        sb.appendLine("[用户身份]")
        if (name.isNotBlank()) sb.appendLine("名字: $name")
        if (description.isNotBlank()) sb.appendLine("描述: $description")
        if (personality.isNotBlank()) sb.appendLine("性格: $personality")
        return sb.toString().trimEnd()
    }
}
