package com.agentapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 代码块背景色
private val CodeBg = Color(0xFFF0EDF5)
private val QuoteBar = Color(0xFFB5A8D5)
private val CodeTextColor = Color(0xFF4A3F5C)

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = parseBlocks(text)

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is Block.Code -> CodeBlock(block.code, block.lang)
                is Block.Text -> InlineMarkdown(block.content)
                is Block.List -> {
                    InlineMarkdown("• ${block.content}")
                }
                is Block.Quote -> QuoteBlock(block.content)
                is Block.Heading -> HeadingBlock(block.level, block.content)
            }
        }
    }
}

// === Block types ===

private sealed class Block {
    data class Code(val code: String, val lang: String = "") : Block()
    data class Text(val content: String) : Block()
    data class List(val content: String) : Block()
    data class Quote(val content: String) : Block()
    data class Heading(val level: Int, val content: String) : Block()
}

private fun parseBlocks(text: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val lines = text.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            // 代码块 ``` ...
            line.trimStart().startsWith("```") -> {
                val lang = line.trimStart().removePrefix("```").trim()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    if (code.isNotEmpty()) code.append("\n")
                    code.append(lines[i])
                    i++
                }
                i++ // skip closing ```
                blocks.add(Block.Code(code.toString(), lang))
            }
            // 标题 #
            line.startsWith("# ") -> { blocks.add(Block.Heading(1, line.removePrefix("# ").trim())); i++ }
            line.startsWith("## ") -> { blocks.add(Block.Heading(2, line.removePrefix("## ").trim())); i++ }
            line.startsWith("### ") -> { blocks.add(Block.Heading(3, line.removePrefix("### ").trim())); i++ }
            // 引用 >
            line.startsWith("> ") -> { blocks.add(Block.Quote(line.removePrefix("> ").trim())); i++ }
            // 无序列表 - 或 *
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                blocks.add(Block.List(line.trimStart().removePrefix("- ").removePrefix("* ").trim()))
                i++
            }
            // 有序列表 1. 2. 等 — 简化处理
            line.matches(Regex("^\\s*\\d+\\.\\s.*")) -> {
                blocks.add(Block.List(line.trimStart().replaceFirst(Regex("^\\d+\\.\\s"), "")))
                i++
            }
            // 空行
            line.isBlank() -> { i++; /* skip */ }
            // 普通文本
            else -> {
                if (blocks.isNotEmpty() && blocks.last() is Block.Text) {
                    // 合并连续文本行
                    val last = blocks.last() as Block.Text
                    blocks[blocks.lastIndex] = Block.Text(last.content + "\n" + line)
                } else {
                    blocks.add(Block.Text(line))
                }
                i++
            }
        }
    }
    return blocks
}

// === Code block ===

@Composable
private fun CodeBlock(code: String, lang: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(CodeBg, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (lang.isNotEmpty()) {
            Text(lang, fontSize = 11.sp, color = Color(0xFF9E8E9E), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
        }
        val scrollState = rememberScrollState()
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = CodeTextColor,
            lineHeight = 18.sp,
            modifier = Modifier.horizontalScroll(scrollState).fillMaxWidth()
        )
    }
}

// === Quote block ===

@Composable
private fun QuoteBlock(content: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Spacer(Modifier.width(4.dp))
        Column(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(QuoteBar, RoundedCornerShape(2.dp))
        ) {}
        Spacer(Modifier.width(8.dp))
        InlineMarkdown(content, Modifier.weight(1f), alpha = 0.8f)
    }
}

// === Heading ===

@Composable
private fun HeadingBlock(level: Int, content: String) {
    val size = when (level) { 1 -> 20.sp; 2 -> 17.sp; else -> 15.sp }
    Text(
        text = parseInlineMarkdown(content),
        fontSize = size,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

// === Inline Markdown Text ===

@Composable
private fun InlineMarkdown(text: String, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    Text(
        text = parseInlineMarkdown(text),
        style = MaterialTheme.typography.bodyMedium.copy(color = color),
        modifier = modifier,
        lineHeight = 22.sp
    )
}

private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // **bold**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                // *italic*
                text.startsWith("*", i) && !text.startsWith("**", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && end + 1 < text.length && text[end + 1] == '*') {
                        // 找到的 * 是 ** 的起始 → 回退，按普通字符处理
                        append(text[i]); i++
                    } else if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                // `inline code`
                text.startsWith("`", i) && !text.startsWith("```", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFFD63384))) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }
}
