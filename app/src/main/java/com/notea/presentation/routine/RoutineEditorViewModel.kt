package com.notea.presentation.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.domain.model.FrequencyType
import com.notea.domain.model.RoutineDraft
import com.notea.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val times: List<String> = listOf("08:00"),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val message: String? = null
) {
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RoutineRepository
) : ViewModel() {

    private val routineIdArg: Long = savedStateHandle["routineId"] ?: -1L
    private val _uiState = MutableStateFlow(RoutineEditorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (routineIdArg > 0L) {
            viewModelScope.launch {
                repository.getRoutineById(routineIdArg)?.let { routine ->
                    _uiState.update {
                        it.copy(
                            routineId = routine.routineId,
                            name = routine.name,
                            description = routine.description.orEmpty(),
                            frequencyType = routine.frequencyType
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(value: String) = _uiState.update { it.copy(name = value) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onFrequencyChanged(value: FrequencyType) = _uiState.update { it.copy(frequencyType = value) }
    fun addTime() = _uiState.update { it.copy(times = it.times + "20:00") }
    fun removeTime(value: String) = _uiState.update { it.copy(times = it.times - value) }
    fun onMessageConsumed() = _uiState.update { it.copy(message = null) }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) {
            _uiState.update { it.copy(message = "La rutina necesita un nombre.") }
            return
        }

        val scheduleMinutes = current.times.mapNotNull {
            try {
                val parts = it.split(":")
                parts[0].toInt() * 60 + parts[1].toInt()
            } catch (_: Exception) {
                null
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.upsert(
                RoutineDraft(
                    routineId = current.routineId,
                    name = current.name,
                    description = current.description,
                    frequencyType = current.frequencyType,
                    scheduleMinutes = scheduleMinutes
                )
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
