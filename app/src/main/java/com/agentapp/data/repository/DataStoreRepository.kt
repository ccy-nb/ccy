package com.agentapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 通用 DataStore 仓库基类
 *
 * 每个子类在文件顶部定义 file-scoped DataStore 委托，传入构造函数。
 * 示例见 PersonaRepository / PresetRepository / RegexRepository。
 */
abstract class DataStoreRepository<T : Any>(
    protected val store: DataStore<Preferences>,
    protected val key: Preferences.Key<String>,
    protected val serializer: KSerializer<T>,
    protected val default: T
) {
    protected val json = Json { ignoreUnknownKeys = true }

    fun flow(): Flow<T> = store.data.map { prefs ->
        val str = prefs[key] ?: return@map default
        try { json.decodeFromString(serializer, str) } catch (_: Exception) { default }
    }

    suspend fun get(): T = flow().first()

    protected suspend fun saveRaw(value: T) {
        store.edit { prefs ->
            prefs[key] = json.encodeToString(serializer, value)
        }
    }
}

/**
 * 单值仓库 — 用于 Persona 等单一对象
 */
abstract class SingleValueRepository<T : Any>(
    store: DataStore<Preferences>,
    key: Preferences.Key<String>,
    serializer: KSerializer<T>,
    default: T
) : DataStoreRepository<T>(store, key, serializer, default) {
    suspend fun save(value: T) = saveRaw(value)
}

/**
 * 列表仓库 — 用于 Preset、RegexScript 等 List<T>
 */
abstract class ListRepository<T : Any>(
    store: DataStore<Preferences>,
    key: Preferences.Key<String>,
    elementSerializer: KSerializer<T>
) : DataStoreRepository<List<T>>(
    store, key,
    kotlinx.serialization.builtins.ListSerializer(elementSerializer),
    emptyList()
) {
    suspend fun list(): List<T> = get()

    suspend fun saveAll(items: List<T>) = saveRaw(items)

    suspend fun saveOne(item: T, idSelector: (T) -> String) {
        val list = list().toMutableList()
        val idx = list.indexOfFirst { idSelector(it) == idSelector(item) }
        if (idx >= 0) list[idx] = item else list.add(item)
        saveAll(list)
    }

    suspend fun delete(id: String, idSelector: (T) -> String) {
        saveAll(list().filter { idSelector(it) != id })
    }
}
