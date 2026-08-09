package com.gatheredthoughts.voicenotes.ui.record

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gatheredthoughts.voicenotes.ui.theme.VoiceNotesTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: RecordViewModel,
    onNavigateToList: () -> Unit,
    onNoteSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onMicPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onMicPermissionResult(granted)
    }

    LaunchedEffect(viewModel) {
        viewModel.onNoteSaved = onNoteSaved
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    if (uiState.showPermissionRationale) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPermissionRationale,
            title = { Text("Microphone Permission") },
            text = {
                Text(
                    "Voice Notes needs microphone access to record and transcribe your voice memos. " +
                        "This data stays on your device except for the categorization API call."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissPermissionRationale()
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPermissionRationale) {
                    Text("Not Now")
                }
            }
        )
    }

    RecordContent(
        uiState = uiState,
        onNavigateToList = onNavigateToList,
        onStartRecording = {
            if (!uiState.hasMicPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                viewModel.startRecording {
                    if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    } else {
                        null
                    }
                }
            }
        },
        onStopRecording = viewModel::stopRecording,
        onDismissPermissionRationale = viewModel::dismissPermissionRationale,
        onGrantPermission = {
            viewModel.dismissPermissionRationale()
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordContent(
    uiState: RecordUiState,
    onNavigateToList: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDismissPermissionRationale: () -> Unit,
    onGrantPermission: () -> Unit
) {
    if (uiState.showPermissionRationale) {
        AlertDialog(
            onDismissRequest = onDismissPermissionRationale,
            title = { Text("Microphone Permission") },
            text = {
                Text(
                    "Voice Notes needs microphone access to record and transcribe your voice memos. " +
                        "This data stays on your device except for the categorization API call."
                )
            },
            confirmButton = {
                TextButton(onClick = onGrantPermission) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPermissionRationale) {
                    Text("Not Now")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record") },
                actions = {
                    IconButton(onClick = onNavigateToList) {
                        Icon(Icons.Default.List, contentDescription = "View notes")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.isSaving) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                FloatingActionButton(
                    onClick = {
                        if (uiState.isRecording) {
                            onStopRecording()
                        } else {
                            onStartRecording()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (uiState.isRecording) "Stop recording" else "Start recording"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            when {
                uiState.isSaving -> {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = "Categorizing your note…",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.finalTranscript.ifBlank { uiState.partialTranscript },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.isRecording -> {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = "Listening…",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = uiState.partialTranscript.ifBlank {
                            "Speak now — your words will appear here."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = "Tap the mic to record a voice note",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your speech is transcribed on-device, then categorized with AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordScreenPreview() {
    VoiceNotesTheme {
        RecordContent(
            uiState = RecordUiState(),
            onNavigateToList = {},
            onStartRecording = {},
            onStopRecording = {},
            onDismissPermissionRationale = {},
            onGrantPermission = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RecordScreenRecordingPreview() {
    VoiceNotesTheme {
        RecordContent(
            uiState = RecordUiState(
                isRecording = true,
                partialTranscript = "Hello, I am recording a note about..."
            ),
            onNavigateToList = {},
            onStartRecording = {},
            onStopRecording = {},
            onDismissPermissionRationale = {},
            onGrantPermission = {}
        )
    }
}
