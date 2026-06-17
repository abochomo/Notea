package com.notea.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.R
import com.notea.domain.model.HomeFilterType
import com.notea.domain.model.Note
import com.notea.domain.model.RecentItem
import com.notea.domain.model.RecentItemType
import com.notea.domain.model.Reminder
import com.notea.domain.model.RoutineSummary
import com.notea.domain.repository.NoteRepository
import com.notea.domain.repository.ReminderRepository
import com.notea.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val selectedFilter: HomeFilterType = HomeFilterType.ALL,
    val notesCount: Int = 0,
    val remindersCount: Int = 0,
    val routinesCount: Int = 0,
    val recentItems: List<RecentItem> = emptyList()
)

private data class HomeSearchResult(
    val notes: List<Note>,
    val reminders: List<Reminder>,
    val routines: List<RoutineSummary>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val filter = MutableStateFlow(HomeFilterType.ALL)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val searchResults = searchQuery
        .map { it.trim() }
        .debounce { query -> if (query.isBlank()) 0L else 250L }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            combine(
                noteRepository.observeActiveNotes(query),
                reminderRepository.observeActiveReminders(query),
                routineRepository.observeRoutines(query)
            ) { notes, reminders, routines ->
                HomeSearchResult(notes = notes, reminders = reminders, routines = routines)
            }
        }

    val uiState = combine(filter, searchResults) { selectedFilter, result ->
        val recents = buildList {
            if (selectedFilter == HomeFilterType.ALL || selectedFilter == HomeFilterType.NOTES) {
                addAll(
                    result.notes.take(8).map {
                        RecentItem(
                            id = it.noteId,
                            type = RecentItemType.NOTE,
                            title = it.title.ifBlank { context.getString(R.string.note_untitled) },
                            subtitle = it.content.take(80),
                            timestamp = it.updatedAt
                        )
                    }
                )
            }
            if (selectedFilter == HomeFilterType.ALL || selectedFilter == HomeFilterType.REMINDERS) {
                addAll(
                    result.reminders.take(8).map {
                        RecentItem(
                            id = it.reminderId,
                            type = RecentItemType.REMINDER,
                            title = it.title,
                            subtitle = it.description.take(80),
                            timestamp = it.updatedAt
                        )
                    }
                )
            }
            if (selectedFilter == HomeFilterType.ALL || selectedFilter == HomeFilterType.ROUTINES) {
                addAll(
                    result.routines.take(8).map {
                        RecentItem(
                            id = it.routineId,
                            type = RecentItemType.ROUTINE,
                            title = it.name,
                            subtitle = it.description.orEmpty().take(80),
                            timestamp = it.updatedAt
                        )
                    }
                )
            }
        }.sortedByDescending { it.timestamp }.take(12)

        HomeUiState(
            selectedFilter = selectedFilter,
            notesCount = result.notes.size,
            remindersCount = result.reminders.size,
            routinesCount = result.routines.size,
            recentItems = recents
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onSearchQueryChanged(value: String) {
        _searchQuery.value = value
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun onFilterChanged(value: HomeFilterType) {
        filter.value = value
    }
}
