package com.agentapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agentapp.data.local.dao.CharacterDao
import com.agentapp.data.local.dao.ChatSessionDao
import com.agentapp.data.local.dao.MessageDao
import com.agentapp.data.local.dao.WorldBookDao
import com.agentapp.data.local.dao.WorldEntryDao
import com.agentapp.data.local.entity.CharacterEntity
import com.agentapp.data.local.entity.ChatSessionEntity
import com.agentapp.data.local.entity.MessageEntity
import com.agentapp.data.local.entity.WorldBookEntity
import com.agentapp.data.local.entity.WorldEntryEntity

@TypeConverters(Converters::class)
@Database(
    entities = [
        CharacterEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        WorldBookEntity::class,
        WorldEntryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun worldBookDao(): WorldBookDao
    abstract fun worldEntryDao(): WorldEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 新建 world_books 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS world_books (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL DEFAULT '',
                        characterId TEXT DEFAULT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                // world_entries 加 worldBookId 列
                db.execSQL("ALTER TABLE world_entries ADD COLUMN worldBookId TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN mesExample TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN creatorNotes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN postHistoryInstructions TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN alternateGreetings TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE characters ADD COLUMN nicknames TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE characters ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE characters ADD COLUMN creator TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN characterVersion TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN spec TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN specVersion TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN talkativeness REAL NOT NULL DEFAULT 0.5")
                db.execSQL("ALTER TABLE characters ADD COLUMN fav INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN depthPrompt TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN worldName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN worldBookEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE world_entries ADD COLUMN priority INTEGER NOT NULL DEFAULT 100")
                db.execSQL("ALTER TABLE world_entries ADD COLUMN characterId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE world_entries ADD COLUMN probability REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE world_entries ADD COLUMN position TEXT NOT NULL DEFAULT 'AFTER_SYSTEM'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agent_app.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
