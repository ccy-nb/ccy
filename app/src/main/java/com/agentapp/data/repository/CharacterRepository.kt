package com.agentapp.data.repository

import android.content.Context
import com.agentapp.data.local.AppDatabase
import com.agentapp.data.local.entity.CharacterEntity
import com.agentapp.data.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
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

    /** 从 JSON 文本导入角色 */
    suspend fun importFromJson(content: String): Character? {
        return try {
            json.decodeFromString<Character>(content)
        } catch (_: Exception) { null }
    }

    /** 从 PNG 角色卡文件导入（SillyTavern 格式）*/
    fun importFromPng(file: File): Character? {
        return try {
            val bytes = file.readBytes()
            val text = extractPngCharaText(bytes)
            if (text != null) json.decodeFromString<Character>(text) else null
        } catch (_: Exception) { null }
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

    /** 从 PNG 字节中提取 SillyTavern 的角色 JSON（tEXt chunk, keyword="chara"）*/
    private fun extractPngCharaText(bytes: ByteArray): String? {
        var pos = 8
        while (pos + 8 <= bytes.size) {
            val length = ((bytes[pos].toInt() and 0xFF) shl 24) or
                         ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                         ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
                         (bytes[pos + 3].toInt() and 0xFF)
            val type = String(bytes, pos + 4, 4, Charsets.US_ASCII)
            val data = pos + 8
            val crc = pos + 8 + length

            if (type == "tEXt" || type == "iTXt") {
                var nullIdx = -1
                for (i in data until crc) { if (bytes[i] == 0.toByte()) { nullIdx = i; break } }
                if (nullIdx in data until crc) {
                    val keyword = String(bytes, data, nullIdx - data, Charsets.US_ASCII)
                    val text = String(bytes, nullIdx + 1, crc - nullIdx - 1, Charsets.UTF_8)
                    if (keyword == "chara") return text
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
