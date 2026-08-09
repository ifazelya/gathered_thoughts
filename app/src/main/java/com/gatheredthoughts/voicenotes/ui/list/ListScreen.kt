package com.gatheredthoughts.voicenotes.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gatheredthoughts.voicenotes.data.NoteEntity
import com.gatheredthoughts.voicenotes.ui.theme.VoiceNotesTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: ListViewModel,
    onNavigateToRecord: () -> Unit,
    onNavigateToQuery: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    ListContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onNavigateToRecord = onNavigateToRecord,
        onNavigateToQuery = onNavigateToQuery,
        onNavigateToDetail = onNavigateToDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListContent(
    uiState: ListUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onNavigateToRecord: () -> Unit,
    onNavigateToQuery: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToRecord) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to record")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToQuery) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Ask Gathered Thoughts")
                    }
                    IconButton(onClick = onNavigateToRecord) {
                        Icon(Icons.Default.Mic, contentDescription = "Record new note")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange
            )

            CategoryFilters(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onCategorySelected
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.notes.isEmpty() -> {
                    EmptyState(
                        hasFilters = uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null
                    )
                }
                else -> {
                    NotesList(
                        notes = uiState.notes,
                        onNoteClick = onNavigateToDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search notes…") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun CategoryFilters(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    val filters = listOf(null to "All") + listOf(
        "Task" to "Task",
        "Idea" to "Idea",
        "Journal" to "Journal",
        "Reminder" to "Reminder"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (value, label) ->
            FilterChip(
                selected = selectedCategory == value,
                onClick = { onCategorySelected(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun EmptyState(hasFilters: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (hasFilters) "No matching notes" else "No notes yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (hasFilters) {
                    "Try a different search or filter."
                } else {
                    "Tap the mic to record your first voice note."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun NotesList(
    notes: List<NoteEntity>,
    onNoteClick: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(note = note, onClick = { onNoteClick(note.id) })
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = note.transcript,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "${note.category} · ${formatTimestamp(note.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
fun ListScreenPreview() {
    VoiceNotesTheme {
        ListContent(
            uiState = ListUiState(
                notes = listOf(
                    NoteEntity(1, "Shopping List", "Buy milk, eggs, and bread.", "Task", System.currentTimeMillis()),
                    NoteEntity(2, "App Idea", "An app that tracks your daily water intake.", "Idea", System.currentTimeMillis() - 86400000)
                ),
                isLoading = false
            ),
            onSearchQueryChange = {},
            onCategorySelected = {},
            onNavigateToRecord = {},
            onNavigateToQuery = {},
            onNavigateToDetail = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListScreenEmptyPreview() {
    VoiceNotesTheme {
        ListContent(
            uiState = ListUiState(notes = emptyList(), isLoading = false),
            onSearchQueryChange = {},
            onCategorySelected = {},
            onNavigateToRecord = {},
            onNavigateToQuery = {},
            onNavigateToDetail = {}
        )
    }
}
