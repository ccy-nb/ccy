package com.agentapp.data.api

import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val temperature: Float,
    val stream: Boolean,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Float? = null,
    @SerialName("presence_penalty") val presencePenalty: Float? = null,
    @SerialName("repetition_penalty") val repetitionPenalty: Float? = null,
    @SerialName("min_p") val minP: Float? = null
)

@Serializable
data class OpenAiResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val error: OpenAiError? = null
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessageContent? = null,
    val delta: OpenAiDelta? = null
)

@Serializable
data class OpenAiMessageContent(val content: String = "")

@Serializable
data class OpenAiDelta(val content: String = "")

@Serializable
data class OpenAiError(val message: String = "")

class OpenAiStrategy : ApiStrategy {
    private val jsonMedia = "application/json".toMediaType()

    private fun buildRequestBody(config: ApiConfig, messages: List<Message>, stream: Boolean): String {
        val chatMessages = messages.map { msg ->
            OpenAiChatMessage(
                role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                },
                content = msg.content
            )
        }
        val body = OpenAiChatRequest(
            model = config.model,
            messages = chatMessages,
            temperature = config.temperature,
            stream = stream,
            maxTokens = if (config.maxTokens > 0) config.maxTokens else null,
            topP = if (config.topP != 1.0f) config.topP else null,
            topK = if (config.topK > 0) config.topK else null,
            frequencyPenalty = if (config.frequencyPenalty != 0f) config.frequencyPenalty else null,
            presencePenalty = if (config.presencePenalty != 0f) config.presencePenalty else null,
            repetitionPenalty = if (config.repetitionPenalty != 1.0f) config.repetitionPenalty else null,
            minP = if (config.minP > 0f) config.minP else null
        )
        return json.encodeToString(body)
    }

    override fun buildStreamRequest(config: ApiConfig, messages: List<Message>): Request {
        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(buildRequestBody(config, messages, true).toRequestBody(jsonMedia))
            .build()
    }

    override fun buildSyncRequest(config: ApiConfig, messages: List<Message>): Request {
        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(buildRequestBody(config, messages, false).toRequestBody(jsonMedia))
            .build()
    }

    override fun buildTestRequest(config: ApiConfig): Request {
        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val body = """{"model":"${config.model}","messages":[{"role":"user","content":"hi"}],"max_tokens":1,"stream":false}"""
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jsonMedia))
            .build()
    }

    override fun parseStreamEvent(event: String, data: String): String? {
        // OpenAI 没有 event 行，只有 data: {...}
        return try {
            val chunk = json.decodeFromString<OpenAiResponse>(data)
            val err = chunk.error?.message
            if (err != null) return "[ERROR: $err]"
            val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
            content.ifEmpty { null }
        } catch (_: Exception) { null }
    }

    override fun parseSyncResponse(body: String): String {
        return try {
            val resp = json.decodeFromString<OpenAiResponse>(body)
            resp.choices.firstOrNull()?.message?.content
                ?: resp.error?.let { "错误: ${it.message}" }
                ?: ""
        } catch (_: Exception) { body }
    }

    override fun parseErrorBody(body: String, code: Int): String {
        return try {
            val resp = json.decodeFromString<OpenAiResponse>(body)
            resp.error?.message ?: "HTTP $code"
        } catch (_: Exception) {
            "HTTP $code: ${body.take(100)}"
        }
    }

    override fun modelsUrl(config: ApiConfig): String =
        "${config.baseUrl.trimEnd('/')}/models"

    override fun buildModelsRequest(config: ApiConfig): Request {
        return Request.Builder()
            .url(modelsUrl(config))
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .build()
    }
}
