package com.agentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agentapp.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun listBySession(sessionId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun listBySessionFlow(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE parentMessageId = :parentId ORDER BY siblingIndex ASC")
    suspend fun getSwipes(parentId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun get(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
