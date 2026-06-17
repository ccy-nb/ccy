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
}
