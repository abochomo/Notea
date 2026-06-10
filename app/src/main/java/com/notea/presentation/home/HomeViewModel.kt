package com.notea.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.domain.model.HomeFilterType
import com.notea.domain.model.RecentItem
import com.notea.domain.model.RecentItemType
import com.notea.domain.repository.NoteRepository
import com.notea.domain.repository.ReminderRepository
import com.notea.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val query: String = "",
    val selectedFilter: HomeFilterType = HomeFilterType.ALL,
    val notesCount: Int = 0,
    val remindersCount: Int = 0,
    val routinesCount: Int = 0,
    val recentItems: List<RecentItem> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(HomeFilterType.ALL)

    val uiState = combine(query, filter) { q, f -> q to f }
        .flatMapLatest { (q, f) ->
            combine(
                noteRepository.observeActiveNotes(if (f == HomeFilterType.ALL || f == HomeFilterType.NOTES) q else ""),
                reminderRepository.observeActiveReminders(if (f == HomeFilterType.ALL || f == HomeFilterType.REMINDERS) q else ""),
                routineRepository.observeRoutines(if (f == HomeFilterType.ALL || f == HomeFilterType.ROUTINES) q else "")
            ) { notes, reminders, routines ->
                val recents = buildList {
                    if (f == HomeFilterType.ALL || f == HomeFilterType.NOTES) {
                        addAll(
                            notes.take(8).map {
                                RecentItem(
                                    id = it.noteId,
                                    type = RecentItemType.NOTE,
                                    title = it.title.ifBlank { "Nota sin título" },
                                    subtitle = it.content.take(80),
                                    timestamp = it.updatedAt
                                )
                            }
                        )
                    }
                    if (f == HomeFilterType.ALL || f == HomeFilterType.REMINDERS) {
                        addAll(
                            reminders.take(8).map {
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
                    if (f == HomeFilterType.ALL || f == HomeFilterType.ROUTINES) {
                        addAll(
                            routines.take(8).map {
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
                    query = q,
                    selectedFilter = f,
                    notesCount = notes.size,
                    remindersCount = reminders.size,
                    routinesCount = routines.size,
                    recentItems = recents
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChanged(value: String) {
        query.value = value
    }

    fun onFilterChanged(value: HomeFilterType) {
        filter.value = value
    }
}
