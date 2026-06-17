package com.agentapp.data.repository

import android.content.Context
import com.agentapp.data.local.AppDatabase
import com.agentapp.data.local.entity.WorldEntryEntity
import com.agentapp.data.model.Message
import com.agentapp.data.model.WorldEntry
import com.agentapp.data.model.WorldEntryPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class WorldRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.worldEntryDao()

    fun listFlow(): Flow<List<WorldEntry>> {
        return dao.listFlow().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun list(): List<WorldEntry> {
        return dao.list().map { it.toDomain() }
    }

    suspend fun save(entry: WorldEntry) {
        dao.save(entry.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /**
     * 根据消息文本和角色 ID 匹配世界书条目。
     * @param messages 当前对话消息
     * @param characterId 当前角色 ID（传 null 则只匹配全局条目）
     * @return 按位置分组的匹配内容，已按优先级排序并过滤了概率
     */
    /**
     * 根据消息文本和角色 ID 匹配世界书条目。
     * @param messages 当前对话消息
     * @param characterId 当前角色 ID（传 null 则只匹配全局条目）
     * @return 按位置分组的匹配内容，已按优先级排序并过滤了概率
     */
    suspend fun matchEntries(
        messages: List<Message>,
        characterId: String? = null
    ): Map<WorldEntryPosition, List<WorldEntry>> {
        val matched = loadAndFilterEntries(messages, characterId)
        return matched.map { it.toDomain() }.groupBy { it.position }
    }

    /**
     * 匹配并返回触发的关键词列表（用于 UI 实时展示）
     */
    suspend fun matchKeywords(messages: List<Message>, characterId: String? = null): List<String> {
        val text = messages.joinToString(" ") { it.content }
        return loadAndFilterEntries(messages, characterId)
            .flatMap { entity ->
                entity.keys.filter { key -> text.contains(key, ignoreCase = true) }
            }
            .distinct()
    }

    /** 加载条目并应用共同过滤：启用态 + 关键词 + 概率 + 优先级排序 */
    private suspend fun loadAndFilterEntries(
        messages: List<Message>,
        characterId: String?
    ): List<WorldEntryEntity> {
        val entries = if (characterId != null) {
            dao.getByCharacter(characterId)
        } else {
            dao.list()
        }
        val text = messages.joinToString(" ") { it.content }
        return entries
            .filter { it.enabled }
            .filter { entity ->
                entity.keys.any { key -> text.contains(key, ignoreCase = true) }
            }
            .filter { entity ->
                entity.probability >= 1.0f || Random.nextFloat() <= entity.probability
            }
            .sortedBy { it.priority }
    }
}

// === Entity ↔ Domain mapping ===

private fun WorldEntryEntity.toDomain() = WorldEntry(
    id = id,
    keys = keys,
    content = content,
    enabled = enabled,
    priority = priority,
    characterId = characterId,
    probability = probability,
    position = try { WorldEntryPosition.valueOf(position) } catch (_: Exception) { WorldEntryPosition.AFTER_SYSTEM },
    createdAt = createdAt
)

private fun WorldEntry.toEntity() = WorldEntryEntity(
    id = id,
    keys = keys,
    content = content,
    enabled = enabled,
    priority = priority,
    characterId = characterId,
    probability = probability,
    position = position.name,
    createdAt = createdAt
)
