package com.agentapp.data.model

import kotlinx.serialization.Serializable

/**
 * 推理预设 —— 保存一组 API 推理参数和系统指令模板，方便在不同场景间切换。
 * 参考 SillyTavern 的 Inference Preset 系统设计。
 */
@Serializable
data class Preset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "默认预设",
    // === 基础参数 ===
    val temperature: Float = 0.7f,
    val maxTokens: Int = 300,          // 回复最大 token 数（非零默认值）
    val maxContext: Int = 4096,         // 上下文窗口大小（token 预算）
    // === 采样参数（OpenAI 兼容） ===
    val topP: Float = 1.0f,
    val topK: Int = 0,                  // 0 = 不限
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    val repetitionPenalty: Float = 1.0f,
    val minP: Float = 0f,
    // === 绑定 ===
    val systemPrompt: String = "",      // 自定义 system prompt 模板
    val model: String = "",            // 空 = 使用全局设置中的模型
    val characterId: String = ""       // 绑定到特定角色（空=全局）
)
