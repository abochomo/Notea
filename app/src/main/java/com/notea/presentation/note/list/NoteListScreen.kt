package com.notea.presentation.note.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.domain.model.Note
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NoteListRoute(
    onBack: () -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    viewModel: NoteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoteListScreen(
        uiState = uiState,
        onBack = onBack,
        onCreateNote = onCreateNote,
        onOpenNote = onOpenNote,
        onFilterChanged = viewModel::onFilterChanged
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    uiState: NoteListUiState,
    onBack: () -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onFilterChanged: (NoteListFilter) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.note_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNote,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.note_list_new_note)) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoteListFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.selectedFilter == filter,
                            onClick = { onFilterChanged(filter) },
                            label = { Text(stringResource(filter.labelRes())) }
                        )
                    }
                }
            }

            if (uiState.isEmpty) {
                item {
                    EmptyNotesCard(filter = uiState.selectedFilter)
                }
            } else {
                items(uiState.notes, key = { it.noteId }) { note ->
                    NoteRow(
                        note = note,
                        onClick = { onOpenNote(note.noteId) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EmptyNotesCard(filter: NoteListFilter) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(filter.emptyMessageRes()),
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.note_list_empty_action))
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
    }

    ListItem(
        headlineContent = {
            Text(note.title.ifBlank { stringResource(R.string.note_untitled) })
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(note.content.ifBlank { stringResource(R.string.note_empty_content) }.take(120))
                Text(
                    text = stringResource(R.string.note_list_updated_at, formatter.format(note.updatedAt)),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        leadingContent = {
            Icon(Icons.Default.Note, contentDescription = null)
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (note.isFavorite) {
                    Icon(Icons.Default.Star, contentDescription = stringResource(R.string.a11y_favorite_note))
                }
                if (note.isArchived) {
                    Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.a11y_archived_note))
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

private fun NoteListFilter.labelRes(): Int = when (this) {
    NoteListFilter.ALL -> R.string.note_list_filter_all
    NoteListFilter.FAVORITES -> R.string.note_list_filter_favorites
    NoteListFilter.ARCHIVED -> R.string.note_list_filter_archived
}

private fun NoteListFilter.emptyMessageRes(): Int = when (this) {
    NoteListFilter.ALL -> R.string.note_list_empty_all
    NoteListFilter.FAVORITES -> R.string.note_list_empty_favorites
    NoteListFilter.ARCHIVED -> R.string.note_list_empty_archived
}
