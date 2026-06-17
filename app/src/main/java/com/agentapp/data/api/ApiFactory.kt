package com.agentapp.data.api

import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.ApiProvider
import com.agentapp.data.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiFactory {
    companion object {
        /** 连接测试用短超时 client */
        val testClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        /** API 调用用长超时 client（支持流式） */
        val apiClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val client get() = Companion.testClient

    private val json = Json { ignoreUnknownKeys = true }

    private fun isOpenAiCompatible(provider: ApiProvider) = when (provider) {
        ApiProvider.DEEPSEEK, ApiProvider.OPENAI, ApiProvider.NVIDIA, ApiProvider.GOOGLE, ApiProvider.GROQ, ApiProvider.CUSTOM -> true
        ApiProvider.CLAUDE -> false
    }

    fun chatStream(config: ApiConfig, messages: List<Message>): Flow<String> {
        return if (isOpenAiCompatible(config.provider)) OpenAiClient(config).chatStream(messages)
        else ClaudeClient(config).chatStream(messages)
    }

    suspend fun chatSync(config: ApiConfig, messages: List<Message>): String {
        return if (isOpenAiCompatible(config.provider)) OpenAiClient(config).chatSync(messages)
        else ClaudeClient(config).chatSync(messages)
    }

    /** 测试 API 连接：先发最小 chat 请求验证 key，成功后拉模型列表 */
    suspend fun testConnection(config: ApiConfig): ConnectionResult {
        return try {
            // 第 1 步：发送最小 chat completion 验证 API Key
            val chatResult = verifyApiKey(config)
            if (chatResult !is ConnectionResult.Success) return chatResult

            // 第 2 步：调 /models 获取模型列表
            val models = fetchModelList(config)
            ConnectionResult.Success(models)
        } catch (e: java.net.UnknownHostException) {
            ConnectionResult.Fail("无法解析域名，请检查网络")
        } catch (e: java.net.SocketTimeoutException) {
            ConnectionResult.Fail("连接超时，请检查地址和网络")
        } catch (e: Exception) {
            ConnectionResult.Fail("连接失败：${e.message ?: "未知错误"}")
        }
    }

    /** 发送一个 max_tokens=1 的 chat 请求验证 API Key 是否有效 */
    private suspend fun verifyApiKey(config: ApiConfig): ConnectionResult {
        return try {
            val bodyJson = if (isOpenAiCompatible(config.provider)) {
                """{"model":"${config.model}","messages":[{"role":"user","content":"hi"}],"max_tokens":1,"stream":false}"""
            } else {
                """{"model":"${config.model}","max_tokens":1,"messages":[{"role":"user","content":"hi"}],"stream":false}"""
            }

            val url = if (isOpenAiCompatible(config.provider)) {
                "${config.baseUrl.trimEnd('/')}/chat/completions"
            } else {
                "${config.baseUrl.trimEnd('/')}/messages"
            }

            val request = Request.Builder().url(url).apply {
                addHeader("Content-Type", "application/json")
                if (isOpenAiCompatible(config.provider)) {
                    addHeader("Authorization", "Bearer ${config.apiKey}")
                } else {
                    addHeader("x-api-key", config.apiKey)
                    addHeader("anthropic-version", "2023-06-01")
                }
                post(bodyJson.toRequestBody("application/json".toMediaType()))
            }.build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            when {
                response.code == 200 -> ConnectionResult.Success(emptyList())
                response.code == 401 || response.code == 403 ->
                    ConnectionResult.Fail("认证失败，API Key 无效")
                response.code == 404 ->
                    ConnectionResult.Fail("地址不正确，请检查 API 地址")
                else -> {
                    val errMsg = try {
                        json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.let { errorObj ->
                            errorObj["message"]?.jsonPrimitive?.content ?: ""
                        } ?: ""
                    } catch (_: Exception) { "" }
                    ConnectionResult.Fail(if (errMsg.isNotBlank()) errMsg else "HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            // 让外层统一处理网络异常
            throw e
        }
    }

    /** 拉取模型列表 */
    private suspend fun fetchModelList(config: ApiConfig): List<String> {
        return try {
            val url = "${config.baseUrl.trimEnd('/')}/models"
            val request = Request.Builder().url(url).apply {
                if (isOpenAiCompatible(config.provider)) addHeader("Authorization", "Bearer ${config.apiKey}")
                else {
                    addHeader("x-api-key", config.apiKey)
                    addHeader("anthropic-version", "2023-06-01")
                }
            }.build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && response.code == 200) {
                val jsonObj = json.parseToJsonElement(body).jsonObject
                val models = jsonObj["data"]?.jsonArray
                models?.mapNotNull { element ->
                    element.jsonObject["id"]?.jsonPrimitive?.content
                }?.sorted() ?: emptyList()
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

sealed class ConnectionResult {
    data class Success(val models: List<String>) : ConnectionResult()
    data class Fail(val reason: String) : ConnectionResult()
}
