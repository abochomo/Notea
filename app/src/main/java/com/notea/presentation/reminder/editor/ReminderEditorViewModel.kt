package com.notea.presentation.reminder.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.domain.model.ReminderDraft
import com.notea.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ReminderEditorUiState(
    val reminderId: Long? = null,
    val title: String = "",
    val description: String = "",
    val date: String = LocalDate.now().toString(),
    val time: String = LocalTime.now().withSecond(0).withNano(0).toString().take(5),
    val notificationsEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val message: String? = null
) {
    val canSave: Boolean get() = title.isNotBlank()
}

@HiltViewModel
class ReminderEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository
) : ViewModel() {

    private val reminderIdArg: Long = savedStateHandle["reminderId"] ?: -1L

    private val _uiState = MutableStateFlow(ReminderEditorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (reminderIdArg > 0L) {
            viewModelScope.launch {
                repository.getReminderById(reminderIdArg)?.let { reminder ->
                    _uiState.update {
                        it.copy(
                            reminderId = reminder.reminderId,
                            title = reminder.title,
                            description = reminder.description,
                            date = reminder.remindAt.toLocalDate().toString(),
                            time = "%02d:%02d".format(reminder.remindAt.hour, reminder.remindAt.minute),
                            notificationsEnabled = reminder.notificationsEnabled
                        )
                    }
                }
            }
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onDateChanged(value: String) = _uiState.update { it.copy(date = value) }
    fun onTimeChanged(value: String) = _uiState.update { it.copy(time = value) }
    fun onNotificationsChanged(value: Boolean) = _uiState.update { it.copy(notificationsEnabled = value) }
    fun onMessageConsumed() = _uiState.update { it.copy(message = null) }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) {
            _uiState.update { it.copy(message = "El recordatorio necesita un título.") }
            return
        }

        val remindAt = try {
            LocalDateTime.of(
                LocalDate.parse(current.date),
                LocalTime.parse(current.time)
            )
        } catch (_: Exception) {
            _uiState.update { it.copy(message = "Usa fecha yyyy-MM-dd y hora HH:mm.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.upsert(
                ReminderDraft(
                    reminderId = current.reminderId,
                    title = current.title,
                    description = current.description,
                    remindAt = remindAt,
                    notificationsEnabled = current.notificationsEnabled
                )
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
