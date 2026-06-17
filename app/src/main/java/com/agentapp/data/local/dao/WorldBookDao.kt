package com.agentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agentapp.data.local.entity.WorldBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldBookDao {
    @Query("SELECT * FROM world_books ORDER BY createdAt DESC")
    fun listFlow(): Flow<List<WorldBookEntity>>

    @Query("SELECT * FROM world_books ORDER BY createdAt DESC")
    suspend fun list(): List<WorldBookEntity>

    @Query("SELECT * FROM world_books WHERE id = :id")
    suspend fun get(id: String): WorldBookEntity?

    @Query("SELECT * FROM world_books WHERE characterId = :characterId")
    suspend fun getByCharacter(characterId: String): WorldBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(book: WorldBookEntity)

    @Query("DELETE FROM world_books WHERE id = :id")
    suspend fun deleteById(id: String)
}
