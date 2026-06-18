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
    val maxTokens: Int = 300,
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val topK: Int = 0,
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    val repetitionPenalty: Float = 1.0f,
    val minP: Float = 0f,
    val maxContext: Int = 4096
)

@Serializable
enum class ApiProvider {
    DEEPSEEK, OPENAI, CLAUDE, NVIDIA, GOOGLE, GROQ, CUSTOM
}
