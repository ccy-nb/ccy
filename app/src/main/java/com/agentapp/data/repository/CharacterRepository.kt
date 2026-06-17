package com.agentapp.data.repository

import android.content.Context
import com.agentapp.data.local.AppDatabase
import com.agentapp.data.local.entity.CharacterEntity
import com.agentapp.data.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class CharacterRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.characterDao()
    private val json = Json { ignoreUnknownKeys = true }

    fun listFlow(): Flow<List<Character>> {
        return dao.listFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun list(): List<Character> {
        return dao.list().map { it.toDomain() }
    }

    suspend fun get(id: String): Character? {
        return dao.get(id)?.toDomain()
    }

    suspend fun save(character: Character) {
        dao.save(character.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /** 从 JSON 文本导入角色 —— 自动适配 SillyTavern V1/V2/V3 和纯 Character 格式 */
    suspend fun importFromJson(content: String): Character? {
        return try {
            parseCharacterJson(content)
        } catch (_: Exception) { null }
    }

    /** 从 PNG 角色卡文件导入（SillyTavern 格式）*/
    fun importFromPng(file: File): Character? {
        return try {
            val bytes = file.readBytes()
            val text = extractPngCharaText(bytes) ?: return null
            parseCharacterJson(text)
        } catch (_: Exception) { null }
    }

    /** 从 PNG 导入角色+世界书，返回完整 ImportResult */
    fun importFromPngWithWorldBook(file: File): ImportResult? {
        return try {
            val bytes = file.readBytes()
            val text = extractPngCharaText(bytes) ?: return null
            parseCharacterWithWorldBook(text, file.absolutePath)
        } catch (_: Exception) { null }
    }

    /** 导入结果：角色 + 世界书条目 */
    data class ImportResult(
        val character: Character,
        val worldEntries: List<com.agentapp.data.model.WorldEntry> = emptyList()
    )

    /** 解析角色 JSON，自动提取 SillyTavern data 包裹层 + character_book + depth_prompt */
    fun parseCharacterWithWorldBook(text: String, imagePath: String? = null): ImportResult? {
        return try {
            val root = json.parseToJsonElement(text).jsonObject
            val inner = root["data"]?.jsonObject ?: root

            // 提取 depth_prompt → 合并到 system prompt
            val depthPrompt = root["extensions"]?.jsonObject
                ?.get("depth_prompt")?.jsonObject
                ?.get("prompt")?.jsonPrimitive?.content ?:
                inner["extensions"]?.jsonObject
                    ?.get("depth_prompt")?.jsonObject
                    ?.get("prompt")?.jsonPrimitive?.content ?: ""

            // 提取 creator_notes
            val creatorNotes = inner["creator_notes"]?.jsonPrimitive?.content
                ?: root["creator_notes"]?.jsonPrimitive?.content
                ?: root["creatorcomment"]?.jsonPrimitive?.content ?: ""

            // 构建完整的 system prompt
            val rawChar = json.decodeFromJsonElement<Character>(inner)
            val extraPrompt = listOfNotNull(
                depthPrompt.takeIf { it.isNotBlank() },
                creatorNotes.takeIf { it.isNotBlank() }
            ).joinToString("\n\n")
            val character = if (extraPrompt.isNotBlank()) {
                rawChar.copy(systemPrompt = if (rawChar.systemPrompt.isNotBlank())
                    rawChar.systemPrompt + "\n\n" + extraPrompt else extraPrompt)
            } else rawChar

            // 设置头像路径
            val finalChar = if (imagePath != null) character.copy(avatarUri = imagePath) else character

            // 提取 character_book → WorldEntry 列表
            val bookJson = root["data"]?.jsonObject?.get("character_book")?.jsonObject
                ?: root["character_book"]?.jsonObject
            val entries = mutableListOf<com.agentapp.data.model.WorldEntry>()
            if (bookJson != null) {
                val bookEntries = bookJson["entries"]?.jsonArray ?: emptyList()
                for (entryEl in bookEntries) {
                    try {
                        val e = entryEl.jsonObject
                        val keys = e["keys"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                        val secondaryKeys = e["secondary_keys"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                        val allKeys = (keys + secondaryKeys).distinct()
                        val content = e["content"]?.jsonPrimitive?.content ?: ""
                        val comment = e["comment"]?.jsonPrimitive?.content ?: ""
                        val enabled = e["constant"]?.jsonPrimitive?.booleanOrNull?.let { !it } ?: true
                        val priority = e["insertion_order"]?.jsonPrimitive?.intOrNull ?: 100
                        val position = when (e["position"]?.jsonPrimitive?.content) {
                            "before_char" -> com.agentapp.data.model.WorldEntryPosition.BEFORE_SYSTEM
                            "after_char" -> com.agentapp.data.model.WorldEntryPosition.AFTER_SYSTEM
                            else -> com.agentapp.data.model.WorldEntryPosition.AFTER_SYSTEM
                        }
                        val probability = e["selective"]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: 1.0f

                        if (allKeys.isNotEmpty() && content.isNotBlank()) {
                            entries.add(com.agentapp.data.model.WorldEntry(
                                keys = allKeys,
                                content = if (comment.isNotBlank()) "$content\n\n($comment)" else content,
                                enabled = enabled,
                                priority = priority,
                                characterId = character.id,
                                probability = probability,
                                position = position
                            ))
                        }
                    } catch (_: Exception) { /* 跳过损坏的条目 */ }
                }
            }

            ImportResult(finalChar, entries)
        } catch (_: Exception) {
            // 回退: 直接解析不带世界书
            try {
                val char = json.decodeFromString<Character>(text)
                ImportResult(char)
            } catch (_: Exception) { null }
        }
    }

    /** 解析角色 JSON，自动提取 SillyTavern data 包裹层（旧接口） */
    private fun parseCharacterJson(text: String): Character? {
        return parseCharacterWithWorldBook(text)?.character
    }

    /** 从文件导入（自动识别 JSON 或 PNG）*/
    fun importFromFile(file: File): Character? {
        return when {
            file.extension.lowercase() == "png" -> importFromPng(file)
            else -> try {
                json.decodeFromString<Character>(file.readText())
            } catch (_: Exception) { null }
        }
    }

    /**
     * 从 PNG 字节中提取 SillyTavern 的角色 JSON。
     * 支持多种格式：
     * - V1/V2: keyword="chara"
     * - V3: keyword="v3chara" (可能 Base64) 或 "v3" (Base64)
     * - 类脑/CCV3: keyword="ccv3" (Base64)
     * - Character.AI 等: keyword="character"
     * - NovelAI/ComfyUI 嵌入: Comment chunk 中搜索角色 JSON
     */
    private fun extractPngCharaText(bytes: ByteArray): String? {
        var pos = 8
        var commentText: String? = null
        while (pos + 8 <= bytes.size) {
            val length = ((bytes[pos].toInt() and 0xFF) shl 24) or
                         ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                         ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
                         (bytes[pos + 3].toInt() and 0xFF)
            val type = String(bytes, pos + 4, 4, Charsets.US_ASCII)
            val dataStart = pos + 8
            val crc = pos + 8 + length

            if (type == "tEXt" || type == "iTXt" || type == "zTXt") {
                var nullIdx = -1
                for (i in dataStart until crc) { if (bytes[i] == 0.toByte()) { nullIdx = i; break } }
                if (nullIdx in dataStart until crc) {
                    val keyword = String(bytes, dataStart, nullIdx - dataStart, Charsets.US_ASCII)
                    val textBytes = bytes.copyOfRange(nullIdx + 1, crc)
                    val text = String(textBytes, Charsets.UTF_8)

                    when (keyword) {
                        "chara", "character" -> {
                            // V2/V3 的 chara 可能存为 Base64（以 'eyJ' 开头）
                            return if (text.startsWith("eyJ")) {
                                try {
                                    String(android.util.Base64.decode(text, android.util.Base64.NO_WRAP), Charsets.UTF_8)
                                } catch (_: Exception) { text }
                            } else text
                        }
                        "v3chara", "ccv3" -> {
                            return try {
                                String(android.util.Base64.decode(text, android.util.Base64.NO_WRAP), Charsets.UTF_8)
                            } catch (_: Exception) { text }
                        }
                        "v3" -> {
                            try {
                                val decoded = android.util.Base64.decode(textBytes, android.util.Base64.NO_WRAP)
                                return String(decoded, Charsets.UTF_8)
                            } catch (_: Exception) { /* 继续搜索 */ }
                        }
                        "Comment" -> { commentText = text }
                    }
                }
            }

            if (type == "IEND") break
            pos = crc + 4
        }

        // 回退：尝试从 Comment chunk 的 JSON 中提取角色数据（某些工具生成此格式）
        if (commentText != null) {
            return try {
                val root = json.parseToJsonElement(commentText).jsonObject
                if (root.containsKey("name") && root.containsKey("description")) {
                    commentText
                } else if (root.containsKey("char_data")) {
                    root["char_data"]?.toString()
                } else null
            } catch (_: Exception) { null }
        }
        return null
    }
}

// === Entity ↔ Domain mapping ===

private fun CharacterEntity.toDomain() = Character(
    id = id,
    name = name,
    description = description,
    personality = personality,
    scenario = scenario,
    greeting = greeting,
    avatarUri = avatarUri,
    systemPrompt = systemPrompt,
    worldBookEnabled = worldBookEnabled,
    createdAt = createdAt
)

private fun Character.toEntity() = CharacterEntity(
    id = id,
    name = name,
    description = description,
    personality = personality,
    scenario = scenario,
    greeting = greeting,
    avatarUri = avatarUri,
    systemPrompt = systemPrompt,
    worldBookEnabled = worldBookEnabled,
    createdAt = createdAt
)
