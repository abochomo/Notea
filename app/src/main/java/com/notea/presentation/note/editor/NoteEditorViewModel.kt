package com.notea.presentation.note.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.domain.model.NoteDraft
import com.notea.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val message: String? = null
) {
    val canSave: Boolean get() = title.isNotBlank() || content.isNotBlank()
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository
) : ViewModel() {

    private val noteIdArg: Long = savedStateHandle["noteId"] ?: -1L

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (noteIdArg > 0L) {
            viewModelScope.launch {
                repository.getNoteById(noteIdArg)?.let { note ->
                    _uiState.update {
                        it.copy(
                            noteId = note.noteId,
                            title = note.title,
                            content = note.content,
                            isFavorite = note.isFavorite,
                            isArchived = note.isArchived
                        )
                    }
                }
            }
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value) }
    fun onContentChanged(value: String) = _uiState.update { it.copy(content = value) }
    fun onFavoriteChanged() = _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    fun onArchivedChanged() = _uiState.update { it.copy(isArchived = !it.isArchived) }
    fun onMessageConsumed() = _uiState.update { it.copy(message = null) }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) {
            _uiState.update { it.copy(message = "La nota debe tener al menos título o contenido.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.upsert(
                NoteDraft(
                    noteId = current.noteId,
                    title = current.title,
                    content = current.content,
                    isFavorite = current.isFavorite,
                    isArchived = current.isArchived
                )
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
