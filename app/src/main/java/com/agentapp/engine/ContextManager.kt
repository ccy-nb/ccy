package com.agentapp.engine

import com.agentapp.data.model.ApiConfig
import com.agentapp.data.model.Character
import com.agentapp.data.model.Message
import com.agentapp.data.model.Persona
import com.agentapp.data.model.Preset
import com.agentapp.data.model.Role
import com.agentapp.data.model.WorldEntry
import com.agentapp.data.model.WorldEntryPosition
import com.agentapp.data.repository.WorldRepository

/**
 * 上下文管理器 — 负责构建 API 消息列表、世界书注入、上下文截断、预设合并
 */
class ContextManager(
    private val worldRepo: WorldRepository,
    private val personaProvider: suspend () -> Persona
) {
    /**
     * 构建发送给 API 的消息列表
     * - 世界书匹配 + 按位置插入
     * - Persona 注入
     * - 角色定义 + 深度提示
     */
    suspend fun buildApiMessages(
        messages: List<Message>,
        character: Character?,
        characterId: String,
        onMatchedKeywords: (List<String>) -> Unit = {}
    ): List<Message> {
        val worldBookEnabled = character?.worldBookEnabled ?: true

        if (!worldBookEnabled) {
            val sysMsg = character?.let { Message(role = Role.SYSTEM, content = it.buildSystemPrompt()) }
            return if (sysMsg != null) listOf(sysMsg) + messages else messages
        }

        // 匹配世界书
        val matchResult = worldRepo.matchEntries(messages, characterId)
        val matchedKeys = worldRepo.matchKeywords(messages, characterId)
        onMatchedKeywords(matchedKeys)

        // 构建 system prompt
        val sysContent = StringBuilder()

        // BEFORE_SYSTEM
        matchResult[WorldEntryPosition.BEFORE_SYSTEM]?.forEach { entry ->
            sysContent.appendLine(entry.content).appendLine()
        }

        // Persona
        try {
            val persona = personaProvider()
            val personaPrompt = persona.buildPrompt()
            if (personaPrompt.isNotEmpty()) {
                sysContent.appendLine(personaPrompt).appendLine()
            }
        } catch (_: Exception) {}

        // 角色定义
        character?.let {
            sysContent.append(it.buildCharacterPrompt()).appendLine()
        }

        // 深度指令
        if (character != null && character.depthPrompt.isNotBlank()) {
            sysContent.appendLine("=== 深度指令 ===")
            sysContent.appendLine(character.depthPrompt).appendLine()
        }

        // AFTER_SYSTEM
        matchResult[WorldEntryPosition.AFTER_SYSTEM]?.forEach { entry ->
            sysContent.appendLine().appendLine("=== 世界观设定 ===").appendLine(entry.content)
        }

        val sysMsg = Message(role = Role.SYSTEM, content = sysContent.toString())
        val apiMsgs = mutableListOf(sysMsg)
        apiMsgs.addAll(messages)

        // 按位置插入世界书条目
        var insertOffset = 0
        for (i in 1 until apiMsgs.size) {
            val msg = apiMsgs[i]
            when (msg.role) {
                Role.USER -> insertWorldEntries(apiMsgs, i, insertOffset, matchResult, WorldEntryPosition.BEFORE_USER)
                Role.ASSISTANT -> insertWorldEntries(apiMsgs, i, insertOffset, matchResult, WorldEntryPosition.BEFORE_ASSISTANT)
                else -> {}
            }
            when (msg.role) {
                Role.USER -> insertWorldEntries(apiMsgs, i, insertOffset, matchResult, WorldEntryPosition.AFTER_USER)
                Role.ASSISTANT -> insertWorldEntries(apiMsgs, i, insertOffset, matchResult, WorldEntryPosition.AFTER_ASSISTANT)
                else -> {}
            }
        }

        return apiMsgs
    }

    private fun insertWorldEntries(
        apiMsgs: MutableList<Message>,
        index: Int,
        offset: Int,
        matchResult: Map<WorldEntryPosition, List<WorldEntry>>,
        position: WorldEntryPosition
    ) {
        val entries = matchResult[position]
        if (!entries.isNullOrEmpty()) {
            val content = entries.joinToString("\n") { it.content }
            apiMsgs.add(index + offset + if (position.name.startsWith("AFTER_")) 1 else 0, Message(role = Role.SYSTEM, content = content))
        }
    }

    /** 合并预设参数到 ApiConfig */
    fun mergePreset(config: ApiConfig, presets: List<Preset>, selectedPresetId: String?): ApiConfig {
        val preset = presets.find { it.id == selectedPresetId } ?: return config
        return config.copy(
            temperature = preset.temperature,
            maxTokens = preset.maxTokens,
            maxContext = preset.maxContext,
            topP = preset.topP,
            topK = preset.topK,
            frequencyPenalty = preset.frequencyPenalty,
            presencePenalty = preset.presencePenalty,
            repetitionPenalty = preset.repetitionPenalty,
            minP = preset.minP,
            model = if (preset.model.isNotBlank()) preset.model else config.model
        )
    }

    /** 按上下文窗口截断历史消息 */
    fun truncateMessages(messages: List<Message>, config: ApiConfig): List<Message> {
        val maxTokens = config.maxTokens
        val maxContext = config.maxContext
        val systemMsgs = messages.filter { it.role == Role.SYSTEM }
        val chatMsgs = messages.filter { it.role != Role.SYSTEM }
        if (chatMsgs.isEmpty()) return messages

        val tokenBudget = maxContext - maxTokens
        if (tokenBudget <= 0) return systemMsgs + chatMsgs.takeLast(10)

        var totalEstimate = 0
        val keepCount = chatMsgs.reversed().indexOfFirst { msg ->
            totalEstimate += (msg.content.length / 2) + 4
            totalEstimate > tokenBudget
        }.let { if (it < 0) chatMsgs.size else it }

        return systemMsgs + chatMsgs.takeLast(maxOf(keepCount, 4))
    }
}
