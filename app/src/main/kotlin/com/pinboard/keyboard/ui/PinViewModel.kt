package com.pinboard.keyboard.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pinboard.keyboard.data.Pin
import com.pinboard.keyboard.data.PinRepository
import com.pinboard.keyboard.util.BackupManager
import com.pinboard.keyboard.util.CATEGORY_ALL
import com.pinboard.keyboard.util.CATEGORY_FAVORITES
import com.pinboard.keyboard.util.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PinViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PinRepository(app)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(CATEGORY_ALL)
    private val _sortMode = MutableStateFlow(SortMode.RECENT)

    val categories: StateFlow<List<String>> = repository.allPins
        .map { pins -> pins.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pins: StateFlow<List<Pin>> = combine(
        repository.allPins,
        _searchQuery,
        _selectedCategory,
        _sortMode
    ) { pins, query, category, sort ->
        var list = pins
        if (category == CATEGORY_FAVORITES) {
            list = list.filter { it.isFavorite }
        } else if (category != CATEGORY_ALL) {
            list = list.filter { it.category == category }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) || it.text.contains(query, ignoreCase = true)
            }
        }
        list = when (sort) {
            SortMode.RECENT -> list.sortedByDescending { it.lastUsed }
            SortMode.USAGE -> list.sortedByDescending { it.useCount }
            SortMode.TITLE -> list.sortedBy { it.title }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun addOrUpdate(pin: Pin) = viewModelScope.launch {
        if (pin.id == 0L) repository.insert(pin) else repository.update(pin)
    }

    fun delete(pin: Pin) = viewModelScope.launch {
        repository.delete(pin)
    }

    fun toggleFavorite(pin: Pin) = viewModelScope.launch {
        repository.update(pin.copy(isFavorite = !pin.isFavorite))
    }

    /** Writes all current pins as JSON to the given SAF uri. */
    fun backupTo(uri: Uri, onResult: (Result<Int>) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val pins = repository.snapshot()
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                out.write(BackupManager.pinsToJson(pins).toByteArray())
            } ?: error("cannot open output stream")
            withContext(Dispatchers.Main) { onResult(Result.success(pins.size)) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onResult(Result.failure(e)) }
        }
    }

    /** Reads a previously exported JSON file and appends its pins to the database. */
    fun restoreFrom(uri: Uri, onResult: (Result<Int>) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("cannot open input stream")
            val pins = BackupManager.jsonToPins(json)
            repository.insertAll(pins)
            withContext(Dispatchers.Main) { onResult(Result.success(pins.size)) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onResult(Result.failure(e)) }
        }
    }
}
