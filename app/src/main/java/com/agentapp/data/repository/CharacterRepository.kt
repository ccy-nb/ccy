package com.agentapp.data.repository

import android.content.Context
import com.agentapp.data.local.AppDatabase
import com.agentapp.data.local.entity.CharacterEntity
import com.agentapp.data.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
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

    /** 解析角色 JSON，自动提取 SillyTavern data 包裹层 */
    private fun parseCharacterJson(text: String): Character? {
        return try {
            val root = json.parseToJsonElement(text).jsonObject
            // SillyTavern V2/V3 格式: { spec, data: { name, ... } }
            val inner = root["data"]?.jsonObject ?: root
            json.decodeFromJsonElement<Character>(inner)
        } catch (_: Exception) {
            // 回退: 直接解析
            json.decodeFromString<Character>(text)
        }
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
     * 支持 V1/V2 (keyword="chara") 和 V3 (keyword="v3chara" 或 base64-encoded "v3")。
     */
    private fun extractPngCharaText(bytes: ByteArray): String? {
        var pos = 8
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

                    when (keyword) {
                        "chara" -> return String(textBytes, Charsets.UTF_8)
                        "v3chara" -> {
                            // V3 有时存为 Base64
                            val decoded = String(textBytes, Charsets.UTF_8)
                            return try {
                                String(android.util.Base64.decode(decoded, android.util.Base64.NO_WRAP), Charsets.UTF_8)
                            } catch (_: Exception) { decoded }
                        }
                        "v3" -> {
                            // V3 格式存为 Base64-encoded JSON
                            try {
                                val decoded = android.util.Base64.decode(textBytes, android.util.Base64.NO_WRAP)
                                return String(decoded, Charsets.UTF_8)
                            } catch (_: Exception) { /* 继续搜索其他 chunk */ }
                        }
                    }
                }
            }

            if (type == "IEND") break
            pos = crc + 4
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
