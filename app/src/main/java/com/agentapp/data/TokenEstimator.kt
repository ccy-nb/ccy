package com.agentapp.data

/** 简单 Token 估算：中文字 ≈ 2 token，英文 ≈ 0.25 token/字符 */
fun estimateTokenCount(text: String): Int {
    var tokens = 0
    for (ch in text) {
        tokens += when {
            ch.code in 0x4E00..0x9FFF || ch.code in 0x3040..0x30FF -> 2  // CJK
            ch.isWhitespace() -> 0
            else -> 1
        }
    }
    return (tokens / 4) + 1  // 英文每 4 字符 1 token
}

/** 估算整个对话的 token 数 */
fun estimateChatTokens(messages: List<com.agentapp.data.model.Message>): Int {
    return messages.sumOf { estimateTokenCount(it.content) }
}

/** 最大 context token（默认，api 配置可覆盖）*/
const val DEFAULT_MAX_CONTEXT = 4096
const val SYSTEM_PROMPT_RESERVE = 512
