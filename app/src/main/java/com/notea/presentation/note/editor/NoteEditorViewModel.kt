package com.notea.presentation.note.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.R
import com.notea.domain.model.NoteDraft
import com.notea.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isMissing: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val message: String? = null
) {
    val canSave: Boolean get() =
        (title.isNotBlank() || content.isNotBlank()) && !isLoading && !isSaving && !isDeleting

    val canDelete: Boolean get() =
        noteId != null && !isLoading && !isSaving && !isDeleting && !isDeleted && !isMissing
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val repository: NoteRepository
) : ViewModel() {

    private val noteIdArg: Long = savedStateHandle["noteId"] ?: -1L
    private val isEditing = noteIdArg > 0L

    private var titleEdited = false
    private var contentEdited = false
    private var favoriteEdited = false
    private var archivedEdited = false

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            noteId = noteIdArg.takeIf { isEditing },
            isLoading = isEditing
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                try {
                    val note = repository.getNoteById(noteIdArg)
                    if (note == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isMissing = true,
                                message = context.getString(R.string.note_message_missing)
                            )
                        }
                        return@launch
                    }

                    _uiState.update { current ->
                        current.copy(
                            noteId = note.noteId,
                            title = if (titleEdited) current.title else note.title,
                            content = if (contentEdited) current.content else note.content,
                            isFavorite = if (favoriteEdited) current.isFavorite else note.isFavorite,
                            isArchived = if (archivedEdited) current.isArchived else note.isArchived,
                            isLoading = false
                        )
                    }
                } catch (_: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = context.getString(R.string.note_message_load_error)
                        )
                    }
                }
            }
        }
    }

    fun onTitleChanged(value: String) {
        titleEdited = true
        _uiState.update { it.copy(title = value) }
    }

    fun onContentChanged(value: String) {
        contentEdited = true
        _uiState.update { it.copy(content = value) }
    }

    fun onFavoriteChanged() {
        favoriteEdited = true
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    fun onArchivedChanged() {
        archivedEdited = true
        _uiState.update { it.copy(isArchived = !it.isArchived) }
    }

    fun onMessageConsumed() = _uiState.update { it.copy(message = null) }

    fun requestDelete() {
        val current = _uiState.value
        if (!current.canDelete) return
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun dismissDeleteConfirmation() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        if (current.isLoading) {
            _uiState.update { it.copy(message = context.getString(R.string.note_message_wait_load)) }
            return
        }
        if (current.title.isBlank() && current.content.isBlank()) {
            _uiState.update { it.copy(message = context.getString(R.string.note_message_empty_required)) }
            return
        }

        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            try {
                if (current.noteId != null && repository.getNoteById(current.noteId) == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isMissing = true,
                            message = context.getString(R.string.note_message_missing)
                        )
                    }
                    return@launch
                }

                val savedId = repository.upsert(
                    NoteDraft(
                        noteId = current.noteId,
                        title = current.title,
                        content = current.content,
                        isFavorite = current.isFavorite,
                        isArchived = current.isArchived
                    )
                )
                _uiState.update {
                    it.copy(
                        noteId = savedId,
                        isSaving = false,
                        isSaved = true,
                        message = context.getString(R.string.note_message_saved)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = context.getString(R.string.note_message_save_error)
                    )
                }
            }
        }
    }

    fun delete() {
        val current = _uiState.value
        if (current.isDeleting || current.isSaving) return

        val noteId = current.noteId
        if (noteId == null) {
            _uiState.update {
                it.copy(
                    showDeleteConfirmation = false,
                    message = context.getString(R.string.note_message_save_before_delete)
                )
            }
            return
        }

        _uiState.update { it.copy(isDeleting = true, message = null) }
        viewModelScope.launch {
            try {
                if (repository.getNoteById(noteId) == null) {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            isDeleted = true,
                            showDeleteConfirmation = false,
                            message = context.getString(R.string.note_message_missing)
                        )
                    }
                    return@launch
                }

                repository.delete(noteId)
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        isDeleted = true,
                        showDeleteConfirmation = false,
                        message = context.getString(R.string.note_message_deleted)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        showDeleteConfirmation = false,
                        message = context.getString(R.string.note_message_delete_error)
                    )
                }
            }
        }
    }
}
