package com.agentapp.data

/**
 * 启发式 Token 估算（近似 tiktoken 结果）
 *
 * 经验近似：
 * - 中文/日文：~1.8 token/字（GPT-4o 实测约 1.5-2.0）
 * - 英文：~0.4 token/字符（单词平均 ~1.3 token）
 * - 数字/代码：~0.25 token/字符（紧凑编码）
 * - 空白符：~0.2 token/字符
 */
fun estimateTokenCount(text: String): Int {
    var tokens = 0.0
    for (ch in text) {
        tokens += when {
            // CJK Unified Ideographs + CJK Extension A + Hiragana + Katakana
            ch.code in 0x4E00..0x9FFF || ch.code in 0x3400..0x4DBF ||
                ch.code in 0x3040..0x30FF || ch.code in 0x2E80..0x2EFF -> 1.8
            // Digits and common code punctuation
            ch.isDigit() || ch in "+-*/=<>!&|^~%[]{}();:@#$\"'`\\" -> 0.25
            // Whitespace
            ch.isWhitespace() -> 0.2
            // Default: regular Latin/ASCII text
            else -> 0.4
        }
    }
    return maxOf(1, tokens.toInt())
}

/** 估算整个对话的 token 数 */
fun estimateChatTokens(messages: List<com.agentapp.data.model.Message>): Int {
    return messages.sumOf { estimateTokenCount(it.content) }
}

/** 最大 context token（默认，api 配置可覆盖）*/
const val DEFAULT_MAX_CONTEXT = 4096
const val SYSTEM_PROMPT_RESERVE = 512
