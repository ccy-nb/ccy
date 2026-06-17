package com.agentapp.data.model

import kotlinx.serialization.Serializable

/**
 * 用户身份（Persona）—— 定义"我是谁"，在聊天中作为 user 的背景信息注入 system prompt。
 * 参考 Tavo 的 Persona 系统设计。
 */
@Serializable
data class Persona(
    val name: String = "",
    val description: String = ""
) {
    fun buildPrompt(): String {
        if (name.isBlank() && description.isBlank()) return ""
        val sb = StringBuilder()
        sb.appendLine("[用户身份]")
        if (name.isNotBlank()) sb.appendLine("名字: $name")
        if (description.isNotBlank()) sb.appendLine("描述: $description")
        return sb.toString().trimEnd()
    }
}
