package com.agentapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentapp.data.model.Character
import com.agentapp.data.model.WorldBook
import com.agentapp.data.model.WorldEntry
import com.agentapp.data.repository.CharacterRepository
import com.agentapp.data.repository.WorldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorldBookViewModel(application: Application) : AndroidViewModel(application) {
    private val worldRepo = WorldRepository(application)
    private val characterRepo = CharacterRepository(application)

    private val _books = MutableStateFlow<List<WorldBook>>(emptyList())
    val books: StateFlow<List<WorldBook>> = _books.asStateFlow()

    private val _selectedBook = MutableStateFlow<WorldBook?>(null)
    val selectedBook: StateFlow<WorldBook?> = _selectedBook.asStateFlow()

    private val _entries = MutableStateFlow<List<WorldEntry>>(emptyList())
    val entries: StateFlow<List<WorldEntry>> = _entries.asStateFlow()

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    init {
        viewModelScope.launch {
            worldRepo.listBooksFlow().collect { _books.value = it }
        }
        viewModelScope.launch {
            characterRepo.listFlow().collect { _characters.value = it }
        }
    }

    /** 选中一个世界书，加载其条目 */
    fun selectBook(book: WorldBook) {
        _selectedBook.value = book
        viewModelScope.launch {
            worldRepo.listEntriesByBookFlow(book.id).collect { list ->
                _entries.value = list
            }
        }
    }

    /** 返回世界书列表 */
    fun deselectBook() {
        _selectedBook.value = null
        _entries.value = emptyList()
    }

    /** 创建独立世界书（不绑定角色）*/
    fun createBook(name: String) {
        viewModelScope.launch {
            worldRepo.saveBook(WorldBook(name = name))
        }
    }

    fun deleteBook(id: String) {
        viewModelScope.launch {
            worldRepo.deleteBook(id)
        }
    }

    fun saveEntry(entry: WorldEntry) {
        viewModelScope.launch {
            worldRepo.save(entry)
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            worldRepo.delete(id)
        }
    }

    /** 导出世界书为 JSON 并通过分享发送 */
    fun exportBook(bookId: String, context: android.content.Context) {
        viewModelScope.launch {
            val book = worldRepo.getBook(bookId) ?: return@launch
            val entries = worldRepo.listEntriesByBook(bookId)
            val json = kotlinx.serialization.json.Json { prettyPrint = true }
            val stEntries = entries.map { e ->
                com.agentapp.data.model.StLoreEntry(
                    keys = e.keys,
                    content = e.content,
                    enabled = e.enabled,
                    priority = e.priority,
                    probability = e.probability,
                    position = when (e.position) {
                        com.agentapp.data.model.WorldEntryPosition.BEFORE_SYSTEM -> "before_character_description"
                        com.agentapp.data.model.WorldEntryPosition.AFTER_SYSTEM -> "after_character_description"
                        com.agentapp.data.model.WorldEntryPosition.BEFORE_USER -> "before_user_query"
                        com.agentapp.data.model.WorldEntryPosition.AFTER_USER -> "after_user_query"
                        else -> "after_character_description"
                    }
                )
            }
            val lorebook = com.agentapp.data.model.StLorebook(
                name = book.name,
                entries = stEntries
            )
            val jsonStr = json.encodeToString(com.agentapp.data.model.StLorebook.serializer(), lorebook)
            try {
                val file = java.io.File(context.cacheDir, "worldbook_${book.name}_${System.currentTimeMillis()}.json")
                file.writeText(jsonStr)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "导出世界书"))
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 从 JSON 文本导入世界书 */
    fun importBookFromJson(jsonStr: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val stLorebook = json.decodeFromString<com.agentapp.data.model.StLorebook>(jsonStr)
                val book = WorldBook(
                    name = stLorebook.name.ifBlank { "导入的世界书" }
                )
                worldRepo.saveBook(book)
                val entries = stLorebook.entries.map { entry ->
                    WorldEntry(
                        worldBookId = book.id,
                        keys = entry.keys,
                        content = entry.content,
                        enabled = entry.enabled,
                        priority = entry.priority,
                        probability = entry.probability,
                        position = when (entry.position) {
                            "before_character_description" -> com.agentapp.data.model.WorldEntryPosition.BEFORE_SYSTEM
                            "after_character_description" -> com.agentapp.data.model.WorldEntryPosition.AFTER_SYSTEM
                            "before_user_query" -> com.agentapp.data.model.WorldEntryPosition.BEFORE_USER
                            "after_user_query" -> com.agentapp.data.model.WorldEntryPosition.AFTER_USER
                            else -> com.agentapp.data.model.WorldEntryPosition.AFTER_SYSTEM
                        }
                    )
                }
                worldRepo.saveAll(entries)
                android.widget.Toast.makeText(context, "✅ 导入「${book.name}」(${entries.size} 条)", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "❌ 导入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
