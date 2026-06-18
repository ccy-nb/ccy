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

    /** 导入结果：角色 + 世界书 + 条目 + 正则脚本 */
    data class ImportResult(
        val character: Character,
        val worldBook: com.agentapp.data.model.WorldBook? = null,
        val worldEntries: List<com.agentapp.data.model.WorldEntry> = emptyList(),
        val regexScripts: List<com.agentapp.data.model.RegexScript> = emptyList()
    )

    /** 解析角色 JSON，自动提取 SillyTavern data 包裹层 + character_book + 全部 V3 字段 */
    fun parseCharacterWithWorldBook(text: String, imagePath: String? = null): ImportResult? {
        return try {
            val root = json.parseToJsonElement(text).jsonObject
            val inner = root["data"]?.jsonObject ?: root
            val rootExt = root["extensions"]?.jsonObject
            val innerExt = inner["extensions"]?.jsonObject

            // 基本解码
            val rawChar = json.decodeFromJsonElement<Character>(inner)

            // 缺失字段从顶层补充
            val creatorNotes = inner["creator_notes"]?.jsonPrimitive?.content
                ?: root["creatorcomment"]?.jsonPrimitive?.content ?: ""

            val depthPrompt = rootExt?.get("depth_prompt")?.jsonObject?.get("prompt")?.jsonPrimitive?.content
                ?: innerExt?.get("depth_prompt")?.jsonObject?.get("prompt")?.jsonPrimitive?.content ?: ""

            val worldName = innerExt?.get("world")?.jsonPrimitive?.content
                ?: rootExt?.get("world")?.jsonPrimitive?.content ?: ""

            val talkativeness = innerExt?.get("talkativeness")?.jsonPrimitive?.doubleOrNull?.toFloat()
                ?: root["talkativeness"]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: 0.5f

            val fav = innerExt?.get("fav")?.jsonPrimitive?.booleanOrNull
                ?: root["fav"]?.jsonPrimitive?.booleanOrNull ?: false

            val specVal = root["spec"]?.jsonPrimitive?.content ?: ""
            val specVersionVal = root["spec_version"]?.jsonPrimitive?.content ?: ""

            // 清理前端专用标签 (我们暂不支持 HTML 渲染)
            val cleanGreeting = if (rawChar.greeting.trim() in listOf("<SetupUI/>", "", "<SetupUI />"))
                "你好，我是${rawChar.name.ifEmpty { "..." }}，很高兴认识你。"
            else rawChar.greeting

            val cleanDepthPrompt = depthPrompt
                .replace(Regex("<StatusPlaceHolder[^>]*/?>"), "(状态面板)")
                .replace(Regex("<SetupUI[^>]*/?>"), "")
                .trim()

            // 提取 v3_style
            val v3Style = innerExt?.get("v3_style")?.jsonPrimitive?.content
                ?: rootExt?.get("v3_style")?.jsonPrimitive?.content ?: ""

            val character = rawChar.copy(
                greeting = cleanGreeting,
                creatorNotes = creatorNotes.ifBlank { rawChar.creatorNotes },
                depthPrompt = cleanDepthPrompt.ifBlank { rawChar.depthPrompt },
                v3Style = v3Style.ifBlank { rawChar.v3Style },
                worldName = worldName.ifBlank { rawChar.worldName },
                talkativeness = if (rawChar.talkativeness == 0.5f) talkativeness else rawChar.talkativeness,
                fav = fav || rawChar.fav,
                spec = specVal.ifBlank { rawChar.spec },
                specVersion = specVersionVal.ifBlank { rawChar.specVersion },
                avatarUri = imagePath ?: rawChar.avatarUri
            )

            // 提取 character_book → WorldEntry 列表
            // V3: data.extensions.character_book, V2: data.character_book, V1: character_book
            val bookJson = innerExt?.get("character_book")?.jsonObject
                ?: inner["character_book"]?.jsonObject
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

                        if (content.isNotBlank()) {
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

            // 创建世界书
            val bookName = character.worldName.ifBlank { "${character.name} 世界书" }
            val worldBook = if (entries.isNotEmpty()) {
                com.agentapp.data.model.WorldBook(
                    name = bookName,
                    characterId = character.id
                )
            } else null

            // 条目关联到世界书
            val linkedEntries = entries.map { it.copy(worldBookId = worldBook?.id) }

            // 提取正则脚本 (regex_scripts)
            val regexScripts = mutableListOf<com.agentapp.data.model.RegexScript>()
            val regexArray = innerExt?.get("regex_scripts")?.jsonArray
                ?: rootExt?.get("regex_scripts")?.jsonArray
            if (regexArray != null) {
                for (scriptEl in regexArray) {
                    try {
                        val s = scriptEl.jsonObject
                        regexScripts.add(com.agentapp.data.model.RegexScript(
                            name = s["scriptName"]?.jsonPrimitive?.content ?: "regex",
                            findRegex = s["findRegex"]?.jsonPrimitive?.content ?: "",
                            replaceString = s["replaceString"]?.jsonPrimitive?.content ?: "",
                            characterId = character.id
                        ))
                    } catch (_: Exception) { }
                }
            }

            ImportResult(character, worldBook, linkedEntries, regexScripts)
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
                                    String(java.util.Base64.getDecoder().decode(text), Charsets.UTF_8)
                                } catch (_: Exception) { text }
                            } else text
                        }
                        "v3chara", "ccv3" -> {
                            return try {
                                String(java.util.Base64.getDecoder().decode(text), Charsets.UTF_8)
                            } catch (_: Exception) { text }
                        }
                        "v3" -> {
                            try {
                                val decoded = java.util.Base64.getDecoder().decode(textBytes)
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
    mesExample = mesExample,
    systemPrompt = systemPrompt,
    creatorNotes = creatorNotes,
    postHistoryInstructions = postHistoryInstructions,
    alternateGreetings = alternateGreetings,
    nicknames = nicknames,
    tags = tags,
    creator = creator,
    characterVersion = characterVersion,
    spec = spec,
    specVersion = specVersion,
    talkativeness = talkativeness,
    fav = fav,
    depthPrompt = depthPrompt,
    worldName = worldName,
    avatarUri = avatarUri,
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
    mesExample = mesExample,
    systemPrompt = systemPrompt,
    creatorNotes = creatorNotes,
    postHistoryInstructions = postHistoryInstructions,
    alternateGreetings = alternateGreetings,
    nicknames = nicknames,
    tags = tags,
    creator = creator,
    characterVersion = characterVersion,
    spec = spec,
    specVersion = specVersion,
    talkativeness = talkativeness,
    fav = fav,
    depthPrompt = depthPrompt,
    v3Style = v3Style,
    worldName = worldName,
    avatarUri = avatarUri,
    worldBookEnabled = worldBookEnabled,
    createdAt = createdAt
)
