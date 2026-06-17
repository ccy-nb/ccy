package com.agentapp.data.api

import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

object HttpClientProvider {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
}

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float,
    val stream: Boolean,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int? = null
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>,
    val error: ErrorDetail? = null
)

@Serializable
data class Choice(
    val message: MessageContent? = null,
    val delta: Delta? = null
)

@Serializable
data class MessageContent(
    val content: String = ""
)

@Serializable
data class Delta(
    val content: String = ""
)

@Serializable
data class ErrorDetail(
    val message: String = ""
)

class OpenAiClient(private val config: ApiConfig) {
    private val client get() = HttpClientProvider.client

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    fun chatStream(messages: List<Message>): Flow<String> = callbackFlow {
        val chatMessages = messages.map { msg ->
            ChatMessage(
                role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                },
                content = msg.content
            )
        }

        val requestBody = ChatCompletionRequest(
            model = config.model,
            messages = chatMessages,
            temperature = config.temperature,
            stream = true,
            maxTokens = if (config.maxTokens > 0) config.maxTokens else null
        )

        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Accept", "text/event-stream")
            .post(json.encodeToString(requestBody).toRequestBody(jsonMedia))
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyStream = response.body?.byteStream()
            if (bodyStream == null) {
                close()
                return@callbackFlow
            }

            val reader = BufferedReader(InputStreamReader(bodyStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (l.startsWith("data: ")) {
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = json.decodeFromString<ChatCompletionResponse>(data)
                        val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                        if (content.isNotEmpty()) {
                            trySend(content)
                        }
                    } catch (_: Exception) {
                        // 单条 SSE 数据解析失败不中断整个流
                        if (data != "[DONE]") trySend("[WARN: 跳过异常数据: ${data.take(80)}]") else {}
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
        val chatMessages = messages.map { msg ->
            ChatMessage(
                role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                },
                content = msg.content
            )
        }

        val requestBody = ChatCompletionRequest(
            model = config.model,
            messages = chatMessages,
            temperature = config.temperature,
            stream = false,
            maxTokens = if (config.maxTokens > 0) config.maxTokens else null
        )

        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(json.encodeToString(requestBody).toRequestBody(jsonMedia))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            response.close()
            val resp = json.decodeFromString<ChatCompletionResponse>(body)
            resp.choices.firstOrNull()?.message?.content ?: resp.error?.let {
                "错误: ${it.message}"
            } ?: ""
        } catch (e: Exception) {
            "网络错误: ${e.message}"
        }
    }
}
