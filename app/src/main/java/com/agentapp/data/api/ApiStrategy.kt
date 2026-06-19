package com.agentapp.data.api

import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Message
import kotlinx.serialization.json.Json

/**
 * API 策略接口 — 每个 API 格式实现一个策略
 */
interface ApiStrategy {
    val json: Json get() = Json { ignoreUnknownKeys = true }

    /** 构建流式请求 */
    fun buildStreamRequest(config: ApiConfig, messages: List<Message>): okhttp3.Request

    /** 构建同步请求 */
    fun buildSyncRequest(config: ApiConfig, messages: List<Message>): okhttp3.Request

    /** 构建连接测试请求 */
    fun buildTestRequest(config: ApiConfig): okhttp3.Request

    /**
     * 解析 SSE 事件
     * @param event 事件类型（Claude 有 event: xxx，OpenAI 为空字符串）
     * @param data SSE data 行内容
     * @return 解析出的文本片段，null 表示跳过该行，"[ERROR: ...]" 表示错误
     */
    fun parseStreamEvent(event: String, data: String): String?

    /** 解析同步响应 */
    fun parseSyncResponse(body: String): String

    /** 解析错误消息 */
    fun parseErrorBody(body: String, code: Int): String

    /** 拉取模型列表的 URL */
    fun modelsUrl(config: ApiConfig): String

    /** 构建拉取模型列表的请求 */
    fun buildModelsRequest(config: ApiConfig): okhttp3.Request
}
