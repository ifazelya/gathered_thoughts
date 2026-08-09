package com.gatheredthoughts.voicenotes.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatheredthoughts.voicenotes.data.NoteCategories
import com.gatheredthoughts.voicenotes.data.NoteEntity
import com.gatheredthoughts.voicenotes.data.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ListUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val isLoading: Boolean = true
)

class ListViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ListUiState> = combine(
        notesRepository.getAllNotes(),
        searchQuery,
        selectedCategory
    ) { notes, query, category ->
        val filtered = notes.filter { note ->
            val matchesCategory = category == null || note.category == category
            val matchesSearch = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.transcript.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        ListUiState(
            notes = filtered,
            searchQuery = query,
            selectedCategory = category,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.update { query }
    }

    fun onCategorySelected(category: String?) {
        selectedCategory.update { category }
    }

    val categoryFilters: List<String?> = listOf(null) + NoteCategories.ALL
}
