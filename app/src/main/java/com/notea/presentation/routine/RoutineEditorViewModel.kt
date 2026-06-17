package com.notea.presentation.routine

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.R
import com.notea.domain.model.FrequencyType
import com.notea.domain.model.RoutineDraft
import com.notea.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineEditorUiState(
    val routineId: Long? = null,
    val name: String = "",
    val description: String = "",
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val selectedWeekdays: Set<Int> = emptySet(),
    val times: List<String> = listOf("08:00"),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isMissing: Boolean = false,
    val message: String? = null
) {
    val canSave: Boolean get() = name.isNotBlank() && !isLoading && !isSaving && !isMissing
}

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val repository: RoutineRepository
) : ViewModel() {

    private val routineIdArg: Long = savedStateHandle["routineId"] ?: -1L
    private val isEditing = routineIdArg > 0L

    private var nameEdited = false
    private var descriptionEdited = false
    private var frequencyEdited = false
    private var weekdaysEdited = false
    private var timesEdited = false

    private val _uiState = MutableStateFlow(
        RoutineEditorUiState(
            routineId = routineIdArg.takeIf { isEditing },
            times = if (isEditing) emptyList() else listOf(DEFAULT_ROUTINE_TIME),
            isLoading = isEditing
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                try {
                    val routine = repository.getRoutineById(routineIdArg)
                    if (routine == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isMissing = true,
                                message = context.getString(R.string.routine_message_missing)
                            )
                        }
                        return@launch
                    }

                    val times = repository.getScheduleMinutesForRoutine(routineIdArg)
                        .map(::formatScheduleTime)
                    val loadedFrequency = routine.frequencyType.takeIf { it.isSupportedRoutineFrequency() }
                        ?: FrequencyType.DAILY
                    val loadedWeekdays = parseWeeklyDays(routine.customRule)

                    _uiState.update {
                        it.copy(
                            routineId = routine.routineId,
                            name = if (nameEdited) it.name else routine.name,
                            description = if (descriptionEdited) it.description else routine.description.orEmpty(),
                            frequencyType = if (frequencyEdited) it.frequencyType else loadedFrequency,
                            selectedWeekdays = if (weekdaysEdited) it.selectedWeekdays else loadedWeekdays,
                            times = if (timesEdited) it.times else times.sortScheduleTimes(),
                            isLoading = false
                        )
                    }
                } catch (_: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isMissing = true,
                            message = context.getString(R.string.routine_message_load_error)
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        nameEdited = true
        _uiState.update { it.copy(name = value) }
    }

    fun onDescriptionChanged(value: String) {
        descriptionEdited = true
        _uiState.update { it.copy(description = value) }
    }

    fun onFrequencyChanged(value: FrequencyType) {
        if (!value.isSupportedRoutineFrequency()) return
        frequencyEdited = true
        _uiState.update { it.copy(frequencyType = value) }
    }

    fun onWeekdayToggled(day: Int) {
        if (day !in 1..7) return
        weekdaysEdited = true
        _uiState.update {
            val updated = if (day in it.selectedWeekdays) {
                it.selectedWeekdays - day
            } else {
                it.selectedWeekdays + day
            }
            it.copy(selectedWeekdays = updated)
        }
    }

    fun addTime(hour: Int, minute: Int) {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || current.isMissing) return

        val time = formatScheduleTime(hour * 60 + minute)
        if (time in current.times) {
            _uiState.update { it.copy(message = context.getString(R.string.routine_message_duplicate_time)) }
            return
        }

        timesEdited = true
        _uiState.update { it.copy(times = (it.times + time).sortScheduleTimes()) }
    }

    fun editTime(currentValue: String, hour: Int, minute: Int) {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || current.isMissing) return

        val newValue = formatScheduleTime(hour * 60 + minute)
        if (newValue != currentValue && newValue in current.times) {
            _uiState.update { it.copy(message = context.getString(R.string.routine_message_duplicate_time)) }
            return
        }

        timesEdited = true
        _uiState.update {
            it.copy(
                times = it.times.map { item ->
                    if (item == currentValue) newValue else item
                }.sortScheduleTimes()
            )
        }
    }

    fun removeTime(value: String) {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || current.isMissing) return

        timesEdited = true
        _uiState.update { it.copy(times = it.times - value) }
    }

    fun onMessageConsumed() = _uiState.update { it.copy(message = null) }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        if (current.isLoading) {
            _uiState.update { it.copy(message = context.getString(R.string.routine_message_wait_load)) }
            return
        }
        if (current.isMissing) {
            _uiState.update { it.copy(message = context.getString(R.string.routine_message_missing)) }
            return
        }
        if (current.name.isBlank()) {
            _uiState.update { it.copy(message = context.getString(R.string.routine_message_name_required)) }
            return
        }
        if (current.frequencyType == FrequencyType.WEEKLY && current.selectedWeekdays.isEmpty()) {
            _uiState.update { it.copy(message = context.getString(R.string.routine_message_weekday_required)) }
            return
        }
        if (current.times.isEmpty()) {
            _uiState.update { it.copy(message = context.getString(R.string.routine_message_time_required)) }
            return
        }

        val scheduleMinutes = current.times.mapNotNull(::parseScheduleTime)
        if (scheduleMinutes.size != current.times.size) {
            _uiState.update { it.copy(message = context.getString(R.string.routine_message_time_invalid)) }
            return
        }

        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            try {
                if (current.routineId != null && repository.getRoutineById(current.routineId) == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isMissing = true,
                            message = context.getString(R.string.routine_message_missing)
                        )
                    }
                    return@launch
                }

                repository.upsert(
                    RoutineDraft(
                        routineId = current.routineId,
                        name = current.name,
                        description = current.description,
                        frequencyType = current.frequencyType,
                        customRule = if (current.frequencyType == FrequencyType.WEEKLY) {
                            formatWeeklyDays(current.selectedWeekdays)
                        } else {
                            null
                        },
                        scheduleMinutes = scheduleMinutes
                    )
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = context.getString(R.string.routine_message_save_error)
                    )
                }
            }
        }
    }
}

private const val DEFAULT_ROUTINE_TIME = "08:00"
val supportedRoutineFrequencies = listOf(FrequencyType.DAILY, FrequencyType.WEEKLY)

private fun formatScheduleTime(minutes: Int): String {
    val normalizedMinutes = minutes.coerceIn(0, 23 * 60 + 59)
    val hours = normalizedMinutes / 60
    val remainingMinutes = normalizedMinutes % 60
    return "${hours.toString().padStart(2, '0')}:${remainingMinutes.toString().padStart(2, '0')}"
}

private fun parseScheduleTime(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null

    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null

    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}

private fun List<String>.sortScheduleTimes(): List<String> =
    distinct().sortedBy { parseScheduleTime(it) ?: Int.MAX_VALUE }

private fun FrequencyType.isSupportedRoutineFrequency(): Boolean =
    this in supportedRoutineFrequencies

private fun parseWeeklyDays(value: String?): Set<Int> =
    value.orEmpty()
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .toSet()

private fun formatWeeklyDays(days: Set<Int>): String =
    days.sorted().joinToString(",")
