package com.notea.presentation.daydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: CalendarRepository
) : ViewModel() {

    private val date = LocalDate.parse(requireNotNull(savedStateHandle["date"]))
    val uiState = repository.observeDay(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
