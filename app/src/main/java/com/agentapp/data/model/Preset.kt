package com.agentapp.data.model

import kotlinx.serialization.Serializable

/**
 * 推理预设 —— 保存一组 API 推理参数和系统指令模板，方便在不同场景间切换。
 * 参考 Tavo 的 Preset 系统设计。
 */
@Serializable
data class Preset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "默认预设",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 0,          // 0 = 不限
    val systemPrompt: String = "",   // 自定义 system prompt 模板
    val model: String = ""           // 空 = 使用全局设置中的模型
)
