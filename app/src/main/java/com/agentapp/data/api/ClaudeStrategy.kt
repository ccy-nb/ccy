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
data class ClaudeChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ClaudeChatRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ClaudeChatMessage>,
    val system: String? = null,
    val temperature: Float,
    val stream: Boolean,
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("top_k") val topK: Int? = null
)

@Serializable
data class ClaudeChunk(
    val type: String = "",
    val delta: ClaudeChunkDelta? = null
)

@Serializable
data class ClaudeChunkDelta(val text: String = "")

@Serializable
data class ClaudeSyncResponse(
    val content: List<ClaudeSyncContent>? = null,
    val error: ClaudeSyncError? = null
)

@Serializable
data class ClaudeSyncContent(val text: String = "")

@Serializable
data class ClaudeSyncError(val message: String = "")

class ClaudeStrategy : ApiStrategy {
    private val jsonMedia = "application/json".toMediaType()

    private fun buildRequestBody(config: ApiConfig, messages: List<Message>, stream: Boolean): String {
        val claudeMessages = messages
            .filter { it.role != Role.SYSTEM }
            .map { msg ->
                ClaudeChatMessage(
                    role = if (msg.role == Role.ASSISTANT) "assistant" else "user",
                    content = msg.content
                )
            }
        val systemContent = messages.filter { it.role == Role.SYSTEM }
            .joinToString("\n") { it.content }

        val body = ClaudeChatRequest(
            model = config.model,
            maxTokens = if (config.maxTokens > 0) config.maxTokens else 4096,
            messages = claudeMessages,
            system = systemContent.ifBlank { null },
            temperature = config.temperature,
            stream = stream,
            topP = if (config.topP != 1.0f) config.topP else null,
            topK = if (config.topK > 0) config.topK else null
        )
        return json.encodeToString(body)
    }

    override fun buildStreamRequest(config: ApiConfig, messages: List<Message>): Request {
        val url = "${config.baseUrl.trimEnd('/')}/messages"
        return Request.Builder()
            .url(url)
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Accept", "text/event-stream")
            .post(buildRequestBody(config, messages, true).toRequestBody(jsonMedia))
            .build()
    }

    override fun buildSyncRequest(config: ApiConfig, messages: List<Message>): Request {
        val url = "${config.baseUrl.trimEnd('/')}/messages"
        return Request.Builder()
            .url(url)
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(buildRequestBody(config, messages, false).toRequestBody(jsonMedia))
            .build()
    }

    override fun buildTestRequest(config: ApiConfig): Request {
        val url = "${config.baseUrl.trimEnd('/')}/messages"
        val body = """{"model":"${config.model}","max_tokens":1,"messages":[{"role":"user","content":"hi"}],"stream":false}"""
        return Request.Builder()
            .url(url)
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body.toRequestBody(jsonMedia))
            .build()
    }

    override fun parseStreamEvent(event: String, data: String): String? {
        return when (event) {
            "error" -> {
                try {
                    val errChunk = json.decodeFromString<ClaudeSyncResponse>(data)
                    "[ERROR: ${errChunk.error?.message ?: data.take(100)}]"
                } catch (_: Exception) {
                    "[ERROR: ${data.take(100)}]"
                }
            }
            "content_block_delta" -> {
                try {
                    val chunk = json.decodeFromString<ClaudeChunk>(data)
                    val text = chunk.delta?.text ?: ""
                    text.ifEmpty { null }
                } catch (_: Exception) { null }
            }
            else -> null
        }
    }

    override fun parseSyncResponse(body: String): String {
        return try {
            val resp = json.decodeFromString<ClaudeSyncResponse>(body)
            resp.content?.firstOrNull()?.text
                ?: resp.error?.let { "错误: ${it.message}" }
                ?: ""
        } catch (_: Exception) { body }
    }

    override fun parseErrorBody(body: String, code: Int): String {
        return try {
            val resp = json.decodeFromString<ClaudeSyncResponse>(body)
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
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .build()
    }
}
