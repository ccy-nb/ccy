package com.agentapp.engine

import com.agentapp.data.api.ApiFactory
import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import com.agentapp.data.model.parseUpdateVariableBlock
import com.agentapp.data.repository.RegexRepository
import com.agentapp.data.repository.VariableRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * 流式控制器 — 负责 API 调用、流式处理、正则替换、变量提取
 *
 * 输出通过 [state] 暴露，调用方收集即可获得流式文本更新。
 */
class StreamController(
    private val apiFactory: ApiFactory = ApiFactory(),
    private val regexRepo: RegexRepository,
    private val varRepo: VariableRepository
) {
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val sendMutex = Mutex()

    /**
     * 流式发送消息，处理正则、变量、Cancel
     *
     * @return StreamingResult — 包含最终文本、提取的变量 ops、错误信息
     */
    suspend fun send(
        config: ApiConfig,
        messages: List<Message>,
        sessionId: String,
        currentVars: JsonObject,
        onChunk: (String) -> Unit = {}
    ): StreamingResult = sendMutex.withLock {
        _isLoading.value = true
        _streamingText.value = ""

        val builder = StringBuilder()
        val baseScripts = regexRepo.list().toMutableList()
        val statusText = if (currentVars.keys.isNotEmpty()) {
            com.agentapp.data.model.formatVariableTree(
                com.agentapp.data.model.flattenVariables(currentVars)
            )
        } else ""

        val scripts = baseScripts.toMutableList()
        if (statusText.isNotEmpty()) {
            val idx = scripts.indexOfFirst { it.findRegex.contains("StatusPlaceHolder") }
            if (idx >= 0) scripts[idx] = scripts[idx].copy(replaceString = "```\n$statusText\n```")
        }

        return try {
            apiFactory.chatStream(config, messages).collect { chunk ->
                builder.append(chunk)
                // 流式显示原始文本（正则只在最终结果应用一次，避免 O(n²)）
                _streamingText.value = builder.toString()
                onChunk(builder.toString())
            }

            val reply = builder.toString()
            if (reply.startsWith("[ERROR:")) {
                val raw = reply.removePrefix("[ERROR: ").removeSuffix("]").trim()
                StreamingResult.Error(raw.ifEmpty { "未知错误" })
            } else {
                val processedReply = regexRepo.applyScripts(reply, scripts)
                val patchOps = parseUpdateVariableBlock(reply)
                StreamingResult.Success(processedReply, patchOps)
            }
        } catch (e: CancellationException) {
            val partial = if (builder.isNotEmpty()) {
                regexRepo.applyScripts(builder.toString(), scripts) + "\n\n(回复未完成)"
            } else ""
            StreamingResult.Cancelled(partial)
        } catch (e: Exception) {
            val detail = when {
                e is java.net.UnknownHostException -> "无法连接服务器，请检查网络和 API 地址"
                e is java.net.SocketTimeoutException -> "连接超时，请检查网络和 API 地址"
                e is java.io.IOException && e.message != null -> e.message!!
                else -> {
                    val msg = e.message ?: ""
                    val cls = e.javaClass.simpleName
                    val cause = e.cause?.let { " ← ${it.javaClass.simpleName}: ${it.message ?: ""}" } ?: ""
                    msg.ifBlank { "$cls$cause" }
                }
            }
            StreamingResult.Error(detail)
        }.also {
            _isLoading.value = false
            _streamingText.value = ""
        }
    }

    fun cancel() {
        // 协程取消由调用方管理，这里只重置 UI 状态
        _isLoading.value = false
        _streamingText.value = ""
    }

    fun resetState() {
        _isLoading.value = false
        _streamingText.value = ""
    }
}

sealed class StreamingResult {
    data class Success(val content: String, val patchOps: List<com.agentapp.data.model.JsonPatchOp> = emptyList()) : StreamingResult()
    data class Error(val message: String) : StreamingResult()
    data class Cancelled(val partialContent: String) : StreamingResult()
}
