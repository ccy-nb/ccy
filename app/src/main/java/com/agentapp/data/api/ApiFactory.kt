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

class ApiFactory {
    companion object {
        val testClient get() = HttpClientProvider.testClient
        val apiClient get() = HttpClientProvider.client
    }

    private fun strategyFor(config: ApiConfig): ApiStrategy = when (config.provider) {
        ApiProvider.CLAUDE -> ClaudeStrategy()
        else -> OpenAiStrategy()
    }

    fun chatStream(config: ApiConfig, messages: List<Message>): Flow<String> {
        val strategy = strategyFor(config)
        return SseClient.stream(
            requestProducer = { strategy.buildStreamRequest(config, messages) },
            onEvent = strategy::parseStreamEvent
        )
    }

    suspend fun chatSync(config: ApiConfig, messages: List<Message>): String {
        val strategy = strategyFor(config)
        val request = strategy.buildSyncRequest(config, messages)
        return try {
            val response = testClient.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            response.close()
            strategy.parseSyncResponse(body)
        } catch (e: Exception) {
            "网络错误: ${e.message}"
        }
    }

    /** 测试 API 连接 */
    suspend fun testConnection(config: ApiConfig): ConnectionResult {
        return try {
            val strategy = strategyFor(config)
            val verifyResult = verifyApiKey(config, strategy)
            if (verifyResult !is ConnectionResult.Success) return verifyResult

            val models = fetchModelList(config, strategy)
            ConnectionResult.Success(models)
        } catch (e: java.net.UnknownHostException) {
            ConnectionResult.Fail("无法解析域名，请检查网络")
        } catch (e: java.net.SocketTimeoutException) {
            ConnectionResult.Fail("连接超时，请检查地址和网络")
        } catch (e: Exception) {
            ConnectionResult.Fail("连接失败：${e.message ?: "未知错误"}")
        }
    }

    private suspend fun verifyApiKey(config: ApiConfig, strategy: ApiStrategy): ConnectionResult {
        if (config.apiKey.isBlank()) return ConnectionResult.Fail("API Key 为空")
        return try {
            val request = strategy.buildTestRequest(config)
            val response = testClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            when {
                response.code == 200 -> ConnectionResult.Success(emptyList())
                response.code == 401 || response.code == 403 ->
                    ConnectionResult.Fail("认证失败，API Key 无效")
                response.code == 404 ->
                    ConnectionResult.Fail("地址不正确，请检查 API 地址")
                else -> {
                    val errMsg = strategy.parseErrorBody(body, response.code)
                    ConnectionResult.Fail(errMsg)
                }
            }
        } catch (e: Exception) { throw e }
    }

    private suspend fun fetchModelList(config: ApiConfig, strategy: ApiStrategy): List<String> {
        return try {
            val request = strategy.buildModelsRequest(config)
            val response = testClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = Json { ignoreUnknownKeys = true }
                val jsonObj = json.parseToJsonElement(body).jsonObject
                jsonObj["data"]?.jsonArray?.mapNotNull { element ->
                    element.jsonObject["id"]?.jsonPrimitive?.content
                }?.sorted() ?: emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }
}

sealed class ConnectionResult {
    data class Success(val models: List<String>) : ConnectionResult()
    data class Fail(val reason: String) : ConnectionResult()
}
