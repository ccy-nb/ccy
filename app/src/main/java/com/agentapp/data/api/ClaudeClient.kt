package com.agentapp.data.api

import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: String
)

@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val messages: List<ClaudeMessage>,
    val system: String? = null,
    val temperature: Float,
    val stream: Boolean
)

@Serializable
data class ClaudeStreamChunk(
    val type: String = "",
    val delta: ClaudeDelta? = null
)

@Serializable
data class ClaudeDelta(
    val text: String = ""
)

@Serializable
data class ClaudeResponse(
    val content: List<ClaudeContent>? = null,
    val error: ClaudeError? = null
)

@Serializable
data class ClaudeContent(
    val text: String = ""
)

@Serializable
data class ClaudeError(
    val message: String = ""
)

class ClaudeClient(private val config: ApiConfig) {
    private val client get() = HttpClientProvider.client

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    fun chatStream(messages: List<Message>): Flow<String> = callbackFlow {
        val claudeMessages = messages
            .filter { it.role != Role.SYSTEM }
            .map { msg ->
                ClaudeMessage(
                    role = if (msg.role == Role.ASSISTANT) "assistant" else "user",
                    content = msg.content
                )
            }

        val systemContent = messages.filter { it.role == Role.SYSTEM }
            .joinToString("\n") { it.content }

        val requestBody = ClaudeRequest(
            model = config.model,
            maxTokens = if (config.maxTokens > 0) config.maxTokens else 4096,
            messages = claudeMessages,
            system = systemContent.ifBlank { null },
            temperature = config.temperature,
            stream = true
        )

        val url = "${config.baseUrl.trimEnd('/')}/messages"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Accept", "text/event-stream")
            .post(json.encodeToString(requestBody).toRequestBody(jsonMedia))
            .build()

        try {
            val response = client.newCall(request).execute()

            // 检查 HTTP 状态码
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val errMsg = try {
                    val errJson = json.decodeFromString<ClaudeResponse>(errorBody)
                    errJson.error?.message ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: ${errorBody.take(100)}"
                }
                trySend("[ERROR: $errMsg]")
                response.close()
                close()
                return@callbackFlow
            }

            val bodyStream = response.body?.byteStream()
            if (bodyStream == null) {
                close()
                return@callbackFlow
            }

            val reader = BufferedReader(InputStreamReader(bodyStream))
            var line: String?
            var currentEvent = ""
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                when {
                    l.startsWith("event: ") -> { currentEvent = l.removePrefix("event: ").trim() }
                    l.startsWith("data: ") -> {
                        val data = l.removePrefix("data: ").trim()
                        // 处理 error 事件
                        if (currentEvent == "error") {
                            try {
                                val errChunk = json.decodeFromString<ClaudeResponse>(data)
                                trySend("[ERROR: ${errChunk.error?.message ?: data.take(100)}]")
                            } catch (_: Exception) {
                                trySend("[ERROR: ${data.take(100)}]")
                            }
                            break
                        }
                        // 只处理 content_block_delta 事件的 data
                        if (currentEvent == "content_block_delta") {
                            try {
                                val chunk = json.decodeFromString<ClaudeStreamChunk>(data)
                                val text = chunk.delta?.text ?: ""
                                if (text.isNotEmpty()) trySend(text)
                            } catch (_: Exception) {
                                trySend("[WARN: 跳过异常数据: ${data.take(80)}]")
                            }
                        }
                        currentEvent = ""
                    }
                }
            }
            reader.close()
            response.close()
            close()
        } catch (e: Exception) {
            trySend("[ERROR: ${e.message}]")
            close()
        }

        awaitClose { }
    }

    suspend fun chatSync(messages: List<Message>): String {
        val claudeMessages = messages
            .filter { it.role != Role.SYSTEM }
            .map { msg ->
                ClaudeMessage(
                    role = if (msg.role == Role.ASSISTANT) "assistant" else "user",
                    content = msg.content
                )
            }

        val systemContent = messages.filter { it.role == Role.SYSTEM }
            .joinToString("\n") { it.content }

        val requestBody = ClaudeRequest(
            model = config.model,
            maxTokens = if (config.maxTokens > 0) config.maxTokens else 4096,
            messages = claudeMessages,
            system = systemContent.ifBlank { null },
            temperature = config.temperature,
            stream = false
        )

        val url = "${config.baseUrl.trimEnd('/')}/messages"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(json.encodeToString(requestBody).toRequestBody(jsonMedia))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            response.close()
            val resp = json.decodeFromString<ClaudeResponse>(body)
            resp.content?.firstOrNull()?.text ?: resp.error?.let {
                "错误: ${it.message}"
            } ?: ""
        } catch (e: Exception) {
            "网络错误: ${e.message}"
        }
    }
}
