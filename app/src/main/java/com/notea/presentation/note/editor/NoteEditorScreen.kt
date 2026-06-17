package com.notea.presentation.note.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R

@Composable
fun NoteEditorRoute(
    onBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            val shouldGoBack = uiState.isSaved || uiState.isDeleted || uiState.isMissing
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.onMessageConsumed()
            if (shouldGoBack) onBack()
        }
    }

    NoteEditorScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTitleChanged = viewModel::onTitleChanged,
        onContentChanged = viewModel::onContentChanged,
        onFavoriteChanged = viewModel::onFavoriteChanged,
        onArchivedChanged = viewModel::onArchivedChanged,
        onDeleteRequested = viewModel::requestDelete,
        onDeleteDismissed = viewModel::dismissDeleteConfirmation,
        onDeleteConfirmed = viewModel::delete,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    uiState: NoteEditorUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onFavoriteChanged: () -> Unit,
    onArchivedChanged: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (uiState.noteId == null) stringResource(R.string.note_editor_new_title) else stringResource(R.string.note_editor_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState.noteId != null) {
                        IconButton(
                            enabled = uiState.canDelete,
                            onClick = onDeleteRequested
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.a11y_delete_note))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.note_editor_loading))
            } else {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving && !uiState.isDeleting,
                    label = { Text(stringResource(R.string.common_title)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = onContentChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving && !uiState.isDeleting,
                    label = { Text(stringResource(R.string.common_content)) },
                    minLines = 8
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        enabled = !uiState.isSaving && !uiState.isDeleting,
                        onClick = onFavoriteChanged,
                        label = { Text(if (uiState.isFavorite) stringResource(R.string.note_favorite) else stringResource(R.string.note_mark_favorite)) },
                        leadingIcon = { Icon(Icons.Default.Star, null) }
                    )
                    AssistChip(
                        enabled = !uiState.isSaving && !uiState.isDeleting,
                        onClick = onArchivedChanged,
                        label = { Text(if (uiState.isArchived) stringResource(R.string.note_archived) else stringResource(R.string.note_archive)) },
                        leadingIcon = { Icon(Icons.Default.Archive, null) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving && !uiState.isDeleting,
                        onClick = onBack,
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) { Text(stringResource(R.string.common_cancel)) }

                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = uiState.canSave,
                        onClick = onSave,
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            when {
                                uiState.isSaving -> stringResource(R.string.common_saving)
                                uiState.isDeleting -> stringResource(R.string.common_deleting)
                                else -> stringResource(R.string.common_save)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text(stringResource(R.string.note_delete_dialog_title)) },
            text = { Text(stringResource(R.string.note_delete_dialog_body)) },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isDeleting,
                    onClick = onDeleteConfirmed
                ) {
                    Text(if (uiState.isDeleting) stringResource(R.string.common_deleting) else stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isDeleting,
                    onClick = onDeleteDismissed
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
