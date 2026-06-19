package com.agentapp.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 统一 SSE 流式解析引擎
 * 负责建立 OkHttp 连接、按行读取 SSE 流、按策略回调解析
 */
object SseClient {

    fun stream(
        requestProducer: () -> okhttp3.Request,
        onEvent: (event: String, data: String) -> String?,
        onError: (String) -> String = { "[ERROR: $it]" }
    ): Flow<String> = callbackFlow {
        var response: Response? = null
        var reader: BufferedReader? = null

        try {
            val request = requestProducer()
            val resp = HttpClientProvider.client.newCall(request).execute()
            response = resp

            if (!resp.isSuccessful) {
                val errorBody = resp.body?.string() ?: ""
                trySend(onError("HTTP ${resp.code}: ${errorBody.take(200)}"))
                return@callbackFlow
            }

            val bodyStream = resp.body?.byteStream() ?: run {
                return@callbackFlow
            }

            reader = BufferedReader(InputStreamReader(bodyStream))
            var currentEvent = ""
            var shouldStop = false

            val lineIter = reader.lineSequence().iterator()
            while (!shouldStop && lineIter.hasNext()) {
                val l = lineIter.next()
                when {
                    l.startsWith("event: ") -> {
                        currentEvent = l.removePrefix("event: ").trim()
                    }
                    l.startsWith("data: ") -> {
                        val data = l.removePrefix("data: ").trim()
                        if (data == "[DONE]") {
                            shouldStop = true
                            continue
                        }
                        val result = onEvent(currentEvent, data)
                        if (result != null) {
                            if (result.startsWith("[ERROR:")) shouldStop = true
                            trySend(result)
                        }
                        currentEvent = ""
                    }
                }
            }
        } catch (e: Exception) {
            trySend(onError("${e.javaClass.simpleName}: ${e.message ?: "null"}"))
        }

        awaitClose {
            reader?.close()
            response?.close()
        }
    }.flowOn(Dispatchers.IO)
}
