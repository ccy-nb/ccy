package com.agentapp.data.repository

import android.content.Context
import com.agentapp.data.local.AppDatabase
import com.agentapp.data.local.entity.ChatSessionEntity
import com.agentapp.data.local.entity.MessageEntity
import com.agentapp.data.model.ChatSession
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val sessionDao = db.chatSessionDao()
    private val messageDao = db.messageDao()

    suspend fun list(characterId: String): List<ChatSession> {
        val sessions = sessionDao.listByCharacter(characterId)
        val results = mutableListOf<ChatSession>()
        for (session in sessions) {
            val messages = messageDao.listBySession(session.id)
            results.add(session.toDomain(messages))
        }
        return results
    }

    /** 响应式会话列表（不含消息内容，消息按需加载） */
    fun listFlow(characterId: String): Flow<List<ChatSession>> {
        return sessionDao.listByCharacterFlow(characterId).map { sessions ->
            sessions.map { it.toDomain() }
        }
    }

    suspend fun get(id: String): ChatSession? {
        val session = sessionDao.get(id) ?: return null
        val messages = messageDao.listBySession(id)
        return session.toDomain(messages)
    }

    fun getMessagesFlow(sessionId: String): Flow<List<Message>> {
        return messageDao.listBySessionFlow(sessionId).map { entities ->
            // 普通消息（parentMessageId == null）按 timestamp 排序
            val roots = entities.filter { it.parentMessageId == null }.sortedBy { it.timestamp }
            // swipe 版本按 parentMessageId 分组
            val swipeMap = entities.filter { it.parentMessageId != null }
                .groupBy { it.parentMessageId }
                .mapValues { (_, swipes) -> swipes.sortedBy { it.siblingIndex } }

            roots.map { root ->
                val swipes = swipeMap[root.id]
                if (swipes != null) {
                    // 有 swipe 版本：root.content 作为第一个版本，swipes 作为备选
                    val allVersions = listOf(root.content) + swipes.map { it.content }
                    val currentSwipeId = root.siblingIndex.coerceIn(0, allVersions.size - 1)
                    root.toDomain().copy(
                        content = allVersions.getOrElse(currentSwipeId) { root.content },
                        swipes = allVersions,
                        currentSwipeId = currentSwipeId
                    )
                } else {
                    root.toDomain()
                }
            }
        }
    }

    suspend fun save(session: ChatSession) {
        sessionDao.save(session.toEntity())
        // 批量 upsert 所有消息（包括 swipe 版本）
        val entities = session.messages.flatMap { msg ->
            val main = msg.toEntity(session.id)
            val swipes = msg.swipes.mapIndexed { index, swipeContent ->
                if (index == msg.currentSwipeId) null else MessageEntity(
                    id = "${msg.id}_swipe_$index",
                    sessionId = session.id,
                    role = "ASSISTANT",
                    content = swipeContent,
                    timestamp = msg.timestamp + index,
                    parentMessageId = msg.id,
                    siblingIndex = index
                )
            }.filterNotNull()
            listOf(main) + swipes
        }
        if (entities.isNotEmpty()) {
            messageDao.saveAll(entities)
        }
        // 清理被删除的消息（不在新列表中的）
        val existingIds = messageDao.listBySession(session.id).map { it.id }.toSet()
        val newIds = entities.map { it.id }.toSet()
        val toDelete = existingIds - newIds
        toDelete.forEach { messageDao.deleteById(it) }
    }

    suspend fun delete(id: String) {
        // 先查实体，再用 @Delete 触发 Room 的 ForeignKey.CASCADE 级联删除消息
        val entity = sessionDao.get(id) ?: return
        sessionDao.delete(entity)
    }

    suspend fun addMessage(sessionId: String, message: Message) {
        messageDao.save(message.toEntity(sessionId))
        // 更新会话的 updatedAt
        val session = sessionDao.get(sessionId) ?: return
        sessionDao.save(session.copy(updatedAt = System.currentTimeMillis()))
    }

    /** 添加一个新的 swipe 版本 */
    suspend fun addSwipe(sessionId: String, parentMessageId: String, content: String): Int {
        val existingSwipes = messageDao.getSwipes(parentMessageId)
        val nextIndex = (existingSwipes.maxOfOrNull { it.siblingIndex } ?: 0) + 1
        val swipeEntity = MessageEntity(
            id = "${parentMessageId}_swipe_$nextIndex",
            sessionId = sessionId,
            role = "ASSISTANT",
            content = content,
            timestamp = System.currentTimeMillis(),
            parentMessageId = parentMessageId,
            siblingIndex = nextIndex
        )
        messageDao.save(swipeEntity)
        // 更新父消息的 siblingIndex 指向新版本
        val parent = messageDao.get(parentMessageId) ?: return nextIndex
        messageDao.save(parent.copy(siblingIndex = nextIndex))
        return nextIndex
    }

    /** 删除一个 swipe 版本 */
    suspend fun deleteSwipe(parentMessageId: String, swipeSiblingIndex: Int) {
        val swipes = messageDao.getSwipes(parentMessageId)
        val target = swipes.find { it.siblingIndex == swipeSiblingIndex } ?: return
        messageDao.deleteById(target.id)
    }

    suspend fun create(characterId: String): ChatSession {
        val session = ChatSession(
            id = "${characterId}_${System.currentTimeMillis()}",
            characterId = characterId
        )
        sessionDao.save(session.toEntity())
        return session
    }
}

// === Entity ↔ Domain mapping ===

private fun ChatSessionEntity.toDomain(messages: List<MessageEntity> = emptyList()) = ChatSession(
    id = id,
    characterId = characterId,
    messages = messages.map { it.toDomain() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun ChatSession.toEntity() = ChatSessionEntity(
    id = id,
    characterId = characterId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun MessageEntity.toDomain() = Message(
    id = id,
    role = try { Role.valueOf(role) } catch (_: Exception) { Role.USER },
    content = content,
    timestamp = timestamp,
    swipes = emptyList(),
    currentSwipeId = siblingIndex.coerceAtLeast(0)
)

private fun Message.toEntity(sessionId: String) = MessageEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    content = if (swipes.isNotEmpty() && currentSwipeId < swipes.size)
        swipes[currentSwipeId] else content,
    timestamp = timestamp,
    parentMessageId = null,
    siblingIndex = currentSwipeId
)
