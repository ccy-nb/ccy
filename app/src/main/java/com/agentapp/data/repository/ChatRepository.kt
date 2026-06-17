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
            entities.map { it.toDomain() }
        }
    }

    suspend fun save(session: ChatSession) {
        sessionDao.save(session.toEntity())
        // 批量 upsert 所有消息
        val entities = session.messages.map { it.toEntity(session.id) }
        if (entities.isNotEmpty()) {
            messageDao.saveAll(entities)
        }
        // 清理被删除的消息（不在新列表中的）
        val existingIds = messageDao.listBySession(session.id).map { it.id }.toSet()
        val newIds = session.messages.map { it.id }.toSet()
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
    timestamp = timestamp
)

private fun Message.toEntity(sessionId: String) = MessageEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    content = content,
    timestamp = timestamp
)
