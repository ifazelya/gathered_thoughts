package com.gatheredthoughts.voicenotes.ui.record

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatheredthoughts.voicenotes.data.CategorizationRepository
import com.gatheredthoughts.voicenotes.data.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordUiState(
    val partialTranscript: String = "",
    val finalTranscript: String = "",
    val isRecording: Boolean = false,
    val isSaving: Boolean = false,
    val hasMicPermission: Boolean = false,
    val showPermissionRationale: Boolean = false,
    val errorMessage: String? = null
)

class RecordViewModel(
    private val notesRepository: NotesRepository,
    private val categorizationRepository: CategorizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    var onNoteSaved: (() -> Unit)? = null

    fun onMicPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasMicPermission = granted,
                showPermissionRationale = !granted,
                errorMessage = if (granted) null else "Microphone permission is required to record voice notes."
            )
        }
    }

    fun dismissPermissionRationale() {
        _uiState.update { it.copy(showPermissionRationale = false) }
    }

    fun startRecording(createRecognizer: () -> SpeechRecognizer?) {
        if (!_uiState.value.hasMicPermission) {
            _uiState.update { it.copy(showPermissionRationale = true) }
            return
        }

        val recognizer = createRecognizer()
        if (recognizer == null) {
            _uiState.update {
                it.copy(errorMessage = "Speech recognition is not available on this device.")
            }
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = recognizer

        _uiState.update {
            it.copy(
                isRecording = true,
                partialTranscript = "",
                finalTranscript = "",
                errorMessage = null
            )
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.setRecognitionListener(createRecognitionListener())
        recognizer.startListening(intent)
    }

    fun stopRecording() {
        speechRecognizer?.stopListening()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech client error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
                    SpeechRecognizer.ERROR_SERVER -> "Speech server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try again."
                    else -> "Speech recognition failed."
                }
                _uiState.update {
                    it.copy(isRecording = false, errorMessage = message)
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()

                _uiState.update {
                    it.copy(
                        finalTranscript = text,
                        partialTranscript = text,
                        isRecording = false
                    )
                }

                if (text.isNotBlank()) {
                    saveNote(text)
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "No speech detected. Try again.")
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) {
                    _uiState.update { it.copy(partialTranscript = text) }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    private fun saveNote(transcript: String) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val result = categorizationRepository.categorizeNote(transcript)
            notesRepository.insertNote(
                transcript = transcript,
                title = result.title,
                category = result.category
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    partialTranscript = "",
                    finalTranscript = ""
                )
            }
            onNoteSaved?.invoke()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
