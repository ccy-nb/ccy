package com.agentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiConfig(
    val id: String = "default",
    val name: String = "DeepSeek",
    val provider: ApiProvider = ApiProvider.DEEPSEEK,
    val baseUrl: String = "https://api.deepseek.com/v1",
    val apiKey: String = "",
    val model: String = "deepseek-chat",
    val maxTokens: Int = 0,
    val temperature: Float = 0.7f
)

@Serializable
enum class ApiProvider {
    DEEPSEEK, OPENAI, CLAUDE, NVIDIA, GOOGLE, GROQ, CUSTOM
}
