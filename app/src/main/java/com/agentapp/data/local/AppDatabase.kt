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
import com.agentapp.data.local.dao.WorldEntryDao
import com.agentapp.data.local.entity.CharacterEntity
import com.agentapp.data.local.entity.ChatSessionEntity
import com.agentapp.data.local.entity.MessageEntity
import com.agentapp.data.local.entity.WorldEntryEntity

@TypeConverters(Converters::class)
@Database(
    entities = [
        CharacterEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        WorldEntryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun worldEntryDao(): WorldEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
