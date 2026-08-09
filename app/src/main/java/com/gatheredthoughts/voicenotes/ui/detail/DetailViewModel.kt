package com.gatheredthoughts.voicenotes.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatheredthoughts.voicenotes.data.NoteCategories
import com.gatheredthoughts.voicenotes.data.NoteEntity
import com.gatheredthoughts.voicenotes.data.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val note: NoteEntity? = null,
    val title: String = "",
    val transcript: String = "",
    val category: String = NoteCategories.JOURNAL,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val errorMessage: String? = null,
    val isDeleted: Boolean = false
)

class DetailViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadNote(noteId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val note = notesRepository.getNoteById(noteId)
            if (note == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Note not found.")
                }
            } else {
                _uiState.update {
                    it.copy(
                        note = note,
                        title = note.title,
                        transcript = note.transcript,
                        category = note.category,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onTranscriptChange(transcript: String) {
        _uiState.update { it.copy(transcript = transcript) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun saveChanges() {
        val current = _uiState.value.note ?: return
        val title = _uiState.value.title.trim()
        val transcript = _uiState.value.transcript.trim()

        if (title.isBlank() || transcript.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title and transcript cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val updated = current.copy(
                title = title,
                transcript = transcript,
                category = _uiState.value.category
            )
            notesRepository.updateNote(updated)
            _uiState.update {
                it.copy(note = updated, isSaving = false)
            }
        }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteNote() {
        val note = _uiState.value.note ?: return
        viewModelScope.launch {
            notesRepository.deleteNote(note)
            _uiState.update { it.copy(showDeleteDialog = false, isDeleted = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
