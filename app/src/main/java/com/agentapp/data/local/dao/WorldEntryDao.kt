package com.agentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agentapp.data.local.entity.WorldEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldEntryDao {
    @Query("SELECT * FROM world_entries ORDER BY priority ASC, createdAt DESC")
    fun listFlow(): Flow<List<WorldEntryEntity>>

    @Query("SELECT * FROM world_entries ORDER BY priority ASC, createdAt DESC")
    suspend fun list(): List<WorldEntryEntity>

    @Query("SELECT * FROM world_entries WHERE (characterId IS NULL OR characterId = :characterId) ORDER BY priority ASC, createdAt DESC")
    suspend fun getByCharacter(characterId: String): List<WorldEntryEntity>

    @Query("SELECT * FROM world_entries WHERE id = :id")
    suspend fun get(id: String): WorldEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: WorldEntryEntity)

    @Delete
    suspend fun delete(entry: WorldEntryEntity)

    @Query("DELETE FROM world_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
