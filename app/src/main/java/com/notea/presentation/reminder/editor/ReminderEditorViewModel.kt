package com.notea.presentation.reminder.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.R
import com.notea.domain.model.ReminderDraft
import com.notea.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderEditorUiState(
    val reminderId: Long? = null,
    val title: String = "",
    val description: String = "",
    val selectedDate: LocalDate? = null,
    val selectedTime: LocalTime? = null,
    val notificationsEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isCompleting: Boolean = false,
    val isDeleted: Boolean = false,
    val isSaved: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val closeAfterMessage: Boolean = false,
    val message: String? = null
) {
    val canSave: Boolean get() =
        title.isNotBlank() && !isCompleted && !isLoading && !isSaving && !isDeleting && !isCompleting

    val canDelete: Boolean get() =
        reminderId != null && !isLoading && !isSaving && !isDeleting && !isCompleting

    val canComplete: Boolean get() =
        reminderId != null && !isCompleted && !isLoading && !isSaving && !isDeleting && !isCompleting

    val isBusy: Boolean get() = isLoading || isSaving || isDeleting || isCompleting
}

@HiltViewModel
class ReminderEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val repository: ReminderRepository,
    private val clock: Clock
) : ViewModel() {

    private val reminderIdArg: Long = savedStateHandle["reminderId"] ?: -1L
    private val isEditing = reminderIdArg > 0L
    private val initialDateTime = LocalDateTime.now(clock).plusHours(1).withSecond(0).withNano(0)

    private var titleEdited = false
    private var descriptionEdited = false
    private var dateEdited = false
    private var timeEdited = false
    private var notificationsEdited = false

    private val _uiState = MutableStateFlow(
        ReminderEditorUiState(
            reminderId = reminderIdArg.takeIf { isEditing },
            selectedDate = initialDateTime.toLocalDate(),
            selectedTime = initialDateTime.toLocalTime(),
            isLoading = isEditing
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                try {
                    val reminder = repository.getReminderById(reminderIdArg)
                    if (reminder == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                closeAfterMessage = true,
                                message = context.getString(R.string.reminder_message_load_error)
                            )
                        }
                        return@launch
                    }

                    val loadedDate = reminder.remindAt.toLocalDate()
                    val loadedTime = reminder.remindAt.toLocalTime().withSecond(0).withNano(0)
                    _uiState.update { current ->
                        current.copy(
                            reminderId = reminder.reminderId,
                            title = if (titleEdited) current.title else reminder.title,
                            description = if (descriptionEdited) current.description else reminder.description,
                            selectedDate = if (dateEdited) current.selectedDate else loadedDate,
                            selectedTime = if (timeEdited) current.selectedTime else loadedTime,
                            notificationsEnabled = if (notificationsEdited) {
                                current.notificationsEnabled
                            } else {
                                reminder.notificationsEnabled
                            },
                            isCompleted = reminder.isCompleted,
                            isLoading = false
                        )
                    }
                } catch (_: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = context.getString(R.string.reminder_message_load_error)
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

    fun onDescriptionChanged(value: String) {
        descriptionEdited = true
        _uiState.update { it.copy(description = value) }
    }

    fun onDateSelected(value: LocalDate) {
        dateEdited = true
        _uiState.update { it.copy(selectedDate = value) }
    }

    fun onTimeSelected(value: LocalTime) {
        timeEdited = true
        _uiState.update { it.copy(selectedTime = value.withSecond(0).withNano(0)) }
    }

    fun onNotificationsChanged(value: Boolean) {
        notificationsEdited = true
        _uiState.update { it.copy(notificationsEnabled = value) }
    }

    fun onMessageConsumed() = _uiState.update { it.copy(message = null, closeAfterMessage = false) }

    fun requestDelete() {
        if (_uiState.value.canDelete) {
            _uiState.update { it.copy(showDeleteConfirmation = true) }
        }
    }

    fun dismissDeleteConfirmation() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        if (current.isCompleted) {
            _uiState.update { it.copy(message = context.getString(R.string.reminder_message_completed)) }
            return
        }
        if (current.isLoading) {
            _uiState.update { it.copy(message = context.getString(R.string.reminder_message_wait_load)) }
            return
        }
        if (current.title.isBlank()) {
            _uiState.update { it.copy(message = context.getString(R.string.reminder_message_title_required)) }
            return
        }

        val selectedDate = current.selectedDate
        val selectedTime = current.selectedTime
        if (selectedDate == null || selectedTime == null) {
            _uiState.update { it.copy(message = context.getString(R.string.reminder_message_datetime_required)) }
            return
        }

        val remindAt = LocalDateTime.of(selectedDate, selectedTime)
        if (!remindAt.isAfter(LocalDateTime.now(clock))) {
            _uiState.update { it.copy(message = context.getString(R.string.reminder_message_future_required)) }
            return
        }

        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            try {
                if (current.reminderId != null && repository.getReminderById(current.reminderId) == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            closeAfterMessage = true,
                            message = context.getString(R.string.reminder_message_missing)
                        )
                    }
                    return@launch
                }

                val savedId = repository.upsert(
                    ReminderDraft(
                        reminderId = current.reminderId,
                        title = current.title,
                        description = current.description,
                        remindAt = remindAt,
                        notificationsEnabled = current.notificationsEnabled
                    )
                )
                _uiState.update {
                    it.copy(
                        reminderId = savedId,
                        isSaving = false,
                        isSaved = true,
                        closeAfterMessage = true,
                        message = context.getString(R.string.reminder_message_saved)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = context.getString(R.string.reminder_message_save_error)
                    )
                }
            }
        }
    }

    fun delete() {
        val current = _uiState.value
        if (current.isDeleting || current.isSaving || current.isCompleting) return

        val reminderId = current.reminderId
        if (reminderId == null) {
            _uiState.update {
                it.copy(
                    showDeleteConfirmation = false,
                    message = context.getString(R.string.reminder_message_save_before_delete)
                )
            }
            return
        }

        _uiState.update { it.copy(isDeleting = true, message = null) }
        viewModelScope.launch {
            try {
                if (repository.getReminderById(reminderId) == null) {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            isDeleted = true,
                            showDeleteConfirmation = false,
                            closeAfterMessage = true,
                            message = context.getString(R.string.reminder_message_missing)
                        )
                    }
                    return@launch
                }

                repository.delete(reminderId)
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        isDeleted = true,
                        showDeleteConfirmation = false,
                        closeAfterMessage = true,
                        message = context.getString(R.string.reminder_message_deleted)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        showDeleteConfirmation = false,
                        message = context.getString(R.string.reminder_message_delete_error)
                    )
                }
            }
        }
    }

    fun markCompleted() {
        val current = _uiState.value
        if (current.isCompleting || current.isSaving || current.isDeleting) return

        val reminderId = current.reminderId
        if (reminderId == null) {
            _uiState.update { it.copy(message = context.getString(R.string.reminder_message_save_before_complete)) }
            return
        }

        _uiState.update { it.copy(isCompleting = true, message = null) }
        viewModelScope.launch {
            try {
                if (repository.getReminderById(reminderId) == null) {
                    _uiState.update {
                        it.copy(
                            isCompleting = false,
                            closeAfterMessage = true,
                            message = context.getString(R.string.reminder_message_missing)
                        )
                    }
                    return@launch
                }

                repository.setCompleted(reminderId, true)
                _uiState.update {
                    it.copy(
                        isCompleting = false,
                        isCompleted = true,
                        closeAfterMessage = true,
                        message = context.getString(R.string.reminder_message_completed_saved)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isCompleting = false,
                        message = context.getString(R.string.reminder_message_complete_error)
                    )
                }
            }
        }
    }
}
