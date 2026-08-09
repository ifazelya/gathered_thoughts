package com.gatheredthoughts.voicenotes.ui.query

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatheredthoughts.voicenotes.data.NoteEntity
import com.gatheredthoughts.voicenotes.data.NotesRepository
import com.gatheredthoughts.voicenotes.data.QueryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QueryUiState(
    val question: String = "",
    val isLoading: Boolean = false,
    val answer: String? = null,
    val matchingNotes: List<NoteEntity> = emptyList(),
    val hasQueried: Boolean = false,
    val errorMessage: String? = null
)

class QueryViewModel(
    private val notesRepository: NotesRepository,
    private val queryRepository: QueryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueryUiState())
    val uiState: StateFlow<QueryUiState> = _uiState.asStateFlow()

    fun onQuestionChange(question: String) {
        _uiState.update { it.copy(question = question) }
    }

    fun submitQuery() {
        val question = _uiState.value.question.trim()
        if (question.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter a question first.") }
            return
        }

        _uiState.update {
            it.copy(isLoading = true, errorMessage = null, answer = null, matchingNotes = emptyList())
        }

        viewModelScope.launch {
            val allNotes = notesRepository.getAllNotesOnce()
            val result = queryRepository.queryNotes(question, allNotes)
            val matchingNotes = result.noteIds.mapNotNull { id ->
                allNotes.find { it.id == id }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasQueried = true,
                    answer = result.answer,
                    matchingNotes = matchingNotes
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
