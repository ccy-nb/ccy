package com.agentapp.data.repository

import android.content.Context
import com.agentapp.data.local.AppDatabase
import com.agentapp.data.local.entity.WorldBookEntity
import com.agentapp.data.local.entity.WorldEntryEntity
import com.agentapp.data.model.Message
import com.agentapp.data.model.WorldBook
import com.agentapp.data.model.WorldEntry
import com.agentapp.data.model.WorldEntryPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class WorldRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.worldBookDao()
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

    /** 批量保存条目，返回成功条数 */
    suspend fun saveAll(entries: List<WorldEntry>): Int {
        return try {
            val entities = entries.map { it.toEntity() }
            dao.saveAll(entities)
            entities.size
        } catch (e: Exception) {
            0
        }
    }

    // === WorldBook CRUD ===

    fun listBooksFlow(): Flow<List<WorldBook>> {
        return bookDao.listFlow().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun listBooks(): List<WorldBook> {
        return bookDao.list().map { it.toDomain() }
    }

    suspend fun getBook(id: String): WorldBook? {
        return bookDao.get(id)?.toDomain()
    }

    suspend fun getBookByCharacter(characterId: String): WorldBook? {
        return bookDao.getByCharacter(characterId)?.toDomain()
    }

    suspend fun saveBook(book: WorldBook) {
        bookDao.save(book.toEntity())
    }

    suspend fun deleteBook(id: String) {
        bookDao.deleteById(id)
    }

    /** 获取某世界书的所有条目（Flow）*/
    fun listEntriesByBookFlow(bookId: String): Flow<List<WorldEntry>> {
        return dao.listByBookFlow(bookId).map { entities -> entities.map { it.toDomain() } }
    }

    /** 获取某世界书的所有条目（一次性）*/
    suspend fun listEntriesByBook(bookId: String): List<WorldEntry> {
        return listEntriesByBookFlow(bookId).first()
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

    /** 加载条目并应用过滤：constant 始终触发，否则按关键词匹配 + 概率 + 优先级排序 */
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
                // 选择词为空 = 始终触发（类似酒馆的 constant: true）
                entity.keys.isEmpty() || entity.keys.any { key -> text.contains(key, ignoreCase = true) }
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
    worldBookId = worldBookId,
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
    worldBookId = worldBookId,
    keys = keys,
    content = content,
    enabled = enabled,
    priority = priority,
    characterId = characterId,
    probability = probability,
    position = position.name,
    createdAt = createdAt
)

private fun WorldBookEntity.toDomain() = WorldBook(
    id = id,
    name = name,
    characterId = characterId,
    createdAt = createdAt
)

private fun WorldBook.toEntity() = WorldBookEntity(
    id = id,
    name = name,
    characterId = characterId,
    createdAt = createdAt
)
