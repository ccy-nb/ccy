package com.agentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val Context.varDataStore: DataStore<Preferences> by preferencesDataStore(name = "char_variables")

class VariableRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private fun varKey(sessionId: String) = stringPreferencesKey("vars_$sessionId")
    }

    fun getFlow(sessionId: String): Flow<JsonObject> {
        return context.varDataStore.data.map { prefs ->
            val str = prefs[varKey(sessionId)] ?: return@map buildJsonObject {}
            try { json.decodeFromString<JsonObject>(str) } catch (_: Exception) { buildJsonObject {} }
        }
    }

    suspend fun get(sessionId: String): JsonObject {
        return getFlow(sessionId).first()
    }

    suspend fun save(sessionId: String, vars: JsonObject) {
        context.varDataStore.edit { prefs ->
            prefs[varKey(sessionId)] = json.encodeToString(JsonObject.serializer(), vars)
        }
    }

    /** 应用 JSONPatch 操作列表，返回更新后的 JsonObject */
    fun applyPatch(current: JsonObject, ops: List<com.agentapp.data.model.JsonPatchOp>): JsonObject {
        val map = current.toMutableMap()
        for (op in ops) {
            try {
                val parts = op.path.split("/").filter { it.isNotEmpty() }
                if (parts.isEmpty()) continue
                val parentParts = parts.dropLast(1)
                val lastKey = parts.last()

                when (op.op) {
                    "replace" -> setNested(map, parts, parseValue(op.value))
                    "delta" -> {
                        val existing = getNested(map, parts)
                        val old = (existing as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                        val delta = op.value?.trim()?.toDoubleOrNull() ?: 0.0
                        setNested(map, parts, JsonPrimitive(old + delta))
                    }
                    "remove" -> removeNested(map, parentParts, lastKey)
                    "insert" -> setNested(map, parts, parseValue(op.value))
                }
            } catch (_: Exception) { }
        }
        return JsonObject(map)
    }

    private fun parseValue(v: String?): JsonElement {
        if (v == null) return JsonNull
        val t = v.trim()
        return when {
            t.isEmpty() || t == "null" -> JsonPrimitive("")
            t.toIntOrNull() != null -> JsonPrimitive(t.toInt())
            t.toDoubleOrNull() != null -> JsonPrimitive(t.toDouble())
            t == "true" -> JsonPrimitive(true)
            t == "false" -> JsonPrimitive(false)
            else -> JsonPrimitive(t)
        }
    }

    private fun setNested(map: MutableMap<String, JsonElement>, parts: List<String>, value: JsonElement) {
        if (parts.size == 1) {
            map[parts[0]] = value
            return
        }
        val key = parts[0]
        val inner = (map[key] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        setNested(inner, parts.drop(1), value)
        map[key] = JsonObject(inner)
    }

    private fun getNested(map: Map<String, JsonElement>, parts: List<String>): JsonElement? {
        var current: JsonElement? = JsonObject(map)
        for (part in parts) {
            current = (current as? JsonObject)?.get(part) ?: return null
        }
        return current
    }

    private fun removeNested(map: MutableMap<String, JsonElement>, parentParts: List<String>, key: String) {
        if (parentParts.isEmpty()) {
            map.remove(key)
            return
        }
        val inner = (getNested(map, parentParts) as? JsonObject)?.toMutableMap() ?: return
        inner.remove(key)
        // 重建父路径
        setNested(map, parentParts, JsonObject(inner))
    }

}
