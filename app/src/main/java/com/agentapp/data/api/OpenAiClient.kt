package com.agentapp.data.api

import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
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
    
    val testClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
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
    val maxTokens: Int? = null,
    @kotlinx.serialization.SerialName("top_p")
    val topP: Float? = null,
    @kotlinx.serialization.SerialName("top_k")
    val topK: Int? = null,
    @kotlinx.serialization.SerialName("frequency_penalty")
    val frequencyPenalty: Float? = null,
    @kotlinx.serialization.SerialName("presence_penalty")
    val presencePenalty: Float? = null,
    @kotlinx.serialization.SerialName("repetition_penalty")
    val repetitionPenalty: Float? = null,
    @kotlinx.serialization.SerialName("min_p")
    val minP: Float? = null
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
            maxTokens = if (config.maxTokens > 0) config.maxTokens else null,
            topP = if (config.topP != 1.0f) config.topP else null,
            topK = if (config.topK > 0) config.topK else null,
            frequencyPenalty = if (config.frequencyPenalty != 0f) config.frequencyPenalty else null,
            presencePenalty = if (config.presencePenalty != 0f) config.presencePenalty else null,
            repetitionPenalty = if (config.repetitionPenalty != 1.0f) config.repetitionPenalty else null,
            minP = if (config.minP > 0f) config.minP else null
        )

        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"

        var response: okhttp3.Response? = null
        var reader: BufferedReader? = null
        try {
            val bodyJson = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(bodyJson.toRequestBody(jsonMedia))
                .build()

            response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val errMsg = try {
                    val errJson = json.decodeFromString<ChatCompletionResponse>(errorBody)
                    errJson.error?.message ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: ${errorBody.take(100)}"
                }
                trySend("[ERROR: $errMsg]")
                return@callbackFlow
            }

            val bodyStream = response.body?.byteStream()
            if (bodyStream == null) {
                return@callbackFlow
            }

            reader = BufferedReader(InputStreamReader(bodyStream))
            var line: String? = null
            var shouldStop = false
            while (!shouldStop && reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (l.startsWith("data: ")) {
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") {
                        shouldStop = true
                        continue
                    }
                    try {
                        val chunk = json.decodeFromString<ChatCompletionResponse>(data)
                        val err = chunk.error?.message
                        if (err != null) {
                            trySend("[ERROR: $err]")
                            shouldStop = true
                            continue
                        }
                        val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                        if (content.isNotEmpty()) {
                            trySend(content)
                        }
                    } catch (_: Exception) { /* skip malformed SSE lines */ }
                }
            }
        } catch (e: Exception) {
            val cls = e.javaClass.simpleName
            val msg = e.message ?: "null"
            val cause = e.cause?.let { " ← ${it.javaClass.simpleName}: ${it.message}" } ?: ""
            trySend("[ERROR: $cls: $msg$cause]")
        } finally {
            reader?.close()
            response?.close()
            close()
        }

        awaitClose {
            // Cleanup if flow is cancelled mid-stream
            reader?.close()
            response?.close()
        }
    }.flowOn(Dispatchers.IO)

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
            maxTokens = if (config.maxTokens > 0) config.maxTokens else null,
            topP = if (config.topP != 1.0f) config.topP else null,
            topK = if (config.topK > 0) config.topK else null,
            frequencyPenalty = if (config.frequencyPenalty != 0f) config.frequencyPenalty else null,
            presencePenalty = if (config.presencePenalty != 0f) config.presencePenalty else null,
            repetitionPenalty = if (config.repetitionPenalty != 1.0f) config.repetitionPenalty else null,
            minP = if (config.minP > 0f) config.minP else null
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
