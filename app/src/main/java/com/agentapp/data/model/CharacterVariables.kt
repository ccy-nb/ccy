package com.agentapp.data.model

import kotlinx.serialization.Serializable

/**
 * JSONPatch 操作 —— 对应 RFC 6902。
 * 支持 op: replace / delta / insert / remove
 */
@Serializable
data class JsonPatchOp(
    val op: String,           // "replace" | "delta" | "insert" | "remove"
    val path: String,         // JSON Pointer 路径，如 "/世界/当前时间"
    val value: String? = null // replace/delta/insert 的值（统一用 String 方便序列化）
)

/**
 * 角色变量树 —— 以 JSON 对象存储，键为中文路径。
 * 示例: { "世界": { "当前时间":"...", "当前地点":"..." }, "摆渡人": { "灵力":85 } }
 */
typealias VariableTree = Map<String, kotlinx.serialization.json.JsonElement>

/**
 * 扁平化变量树为 路径→值 映射（从 VariableRepository 提取的纯函数）
 */
fun flattenVariables(vars: Map<String, kotlinx.serialization.json.JsonElement>, prefix: String = ""): Map<String, String> {
    val result = LinkedHashMap<String, String>()
    for ((key, value) in vars) {
        val path = if (prefix.isEmpty()) key else "$prefix/$key"
        when (value) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                val v = value.content
                if (v.isNotEmpty()) result[path] = v
            }
            is kotlinx.serialization.json.JsonObject -> result.putAll(flattenVariables(value, path))
            else -> {}
        }
    }
    return result
}

/**
 * 解析 <UpdateVariable> 块中的 JSONPatch 数组。
 * 返回操作列表，或空列表。
 */
fun parseUpdateVariableBlock(text: String): List<JsonPatchOp> {
    val regex = Regex("<UpdateVariable>.*?<JSONPatch>\\s*(\\[.*?])\\s*</JSONPatch>.*?</UpdateVariable>", RegexOption.DOT_MATCHES_ALL)
    val match = regex.find(text) ?: return emptyList()
    val jsonStr = match.groupValues[1]
    return try {
        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString<List<JsonPatchOp>>(jsonStr)
    } catch (_: Exception) { emptyList() }
}

/**
 * 从 <UpdateVariable> 块中提取 <Analysis> 内容（用于日志/调试）。
 */
fun parseUpdateVariableAnalysis(text: String): String {
    val regex = Regex("<Analysis>(.*?)</Analysis>", RegexOption.DOT_MATCHES_ALL)
    return regex.find(text)?.groupValues?.get(1)?.trim() ?: ""
}

/**
 * 将变量树格式化为纯文本表格（用于 StatusPlaceHolder 渲染）。
 */
fun formatVariableTree(vars: Map<String, String>): String {
    if (vars.isEmpty()) return ""
    val sb = StringBuilder()
    sb.appendLine("═══ 当前状态 ═══")
    // 按路径分组显示
    val groups = LinkedHashMap<String, MutableList<Pair<String, String>>>()
    for ((path, value) in vars.entries) {
        val parts = path.split("/").filter { it.isNotEmpty() }
        val group = if (parts.size >= 2) parts[0] else "其他"
        val label = parts.drop(1).joinToString("·")
        groups.getOrPut(group) { mutableListOf() }.add(label to value)
    }
    for ((group, items) in groups) {
        sb.appendLine("【$group】")
        for ((label, value) in items) {
            sb.appendLine("  $label: $value")
        }
    }
    return sb.toString().trimEnd()
}
