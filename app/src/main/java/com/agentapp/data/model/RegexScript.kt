package com.agentapp.data.model

import kotlinx.serialization.Serializable

/**
 * 正则替换脚本 —— 对应酒馆的 regex_scripts。
 * 在 AI 回复文本上运行正则查找+替换，用于渲染前端组件。
 */
@Serializable
data class RegexScript(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val findRegex: String = "",       // 正则查找模式
    val replaceString: String = "",   // 替换文本（支持 $1 捕获组引用）
    val characterId: String? = null,  // null=全局，非null=绑定角色
    val enabled: Boolean = true,
    val priority: Int = 100
)
