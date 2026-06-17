package com.notea.presentation.note.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.domain.model.Note
import com.notea.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class NoteListFilter {
    ALL, FAVORITES, ARCHIVED
}

data class NoteListUiState(
    val selectedFilter: NoteListFilter = NoteListFilter.ALL,
    val notes: List<Note> = emptyList()
) {
    val isEmpty: Boolean get() = notes.isEmpty()
}

@HiltViewModel
class NoteListViewModel @Inject constructor(
    noteRepository: NoteRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(NoteListFilter.ALL)

    val uiState = combine(
        selectedFilter,
        noteRepository.observeAllNotes()
    ) { filter, notes ->
        NoteListUiState(
            selectedFilter = filter,
            notes = when (filter) {
                NoteListFilter.ALL -> notes
                NoteListFilter.FAVORITES -> notes.filter { it.isFavorite }
                NoteListFilter.ARCHIVED -> notes.filter { it.isArchived }
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteListUiState()
    )

    fun onFilterChanged(filter: NoteListFilter) {
        selectedFilter.value = filter
    }
}
