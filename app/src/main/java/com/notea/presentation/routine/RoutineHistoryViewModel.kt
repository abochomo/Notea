package com.notea.presentation.routine

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.R
import com.notea.domain.model.QuestionFieldType
import com.notea.domain.model.RoutineAnswer
import com.notea.domain.model.RoutineExecution
import com.notea.domain.model.RoutineExecutionStatus
import com.notea.domain.model.RoutineQuestion
import com.notea.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineHistoryUiState(
    val routineId: Long = -1L,
    val routineName: String = "",
    val executions: List<RoutineHistoryExecutionItem> = emptyList(),
    val selectedExecutionId: Long? = null,
    val selectedAnswers: List<RoutineHistoryAnswerItem> = emptyList(),
    val isLoading: Boolean = true,
    val isDetailLoading: Boolean = false,
    val message: String? = null
)

data class RoutineHistoryExecutionItem(
    val executionId: Long,
    val date: LocalDate,
    val status: RoutineExecutionStatus
)

data class RoutineHistoryAnswerItem(
    val questionLabel: String,
    val fieldType: QuestionFieldType,
    val value: String
)

@HiltViewModel
class RoutineHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val repository: RoutineRepository
) : ViewModel() {

    private val routineIdArg: Long = savedStateHandle["routineId"] ?: -1L
    private val executionIdArg: Long = savedStateHandle["executionId"] ?: -1L

    private val _uiState = MutableStateFlow(RoutineHistoryUiState(routineId = routineIdArg))
    val uiState = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun onExecutionSelected(executionId: Long) {
        val current = _uiState.value
        if (current.isLoading || current.isDetailLoading) return

        loadExecutionAnswers(executionId)
    }

    private fun loadExecutionAnswers(executionId: Long) {
        _uiState.update {
            it.copy(
                selectedExecutionId = executionId,
                selectedAnswers = emptyList(),
                isDetailLoading = true,
                message = null
            )
        }

        viewModelScope.launch {
            try {
                val questions = repository.getQuestionsForRoutine(routineIdArg)
                val answersByQuestion = repository.getAnswersForExecution(executionId)
                    .associateBy { it.questionId }
                val answerItems = questions.map { question ->
                    question.toHistoryAnswerItem(answersByQuestion[question.questionId], context)
                }

                _uiState.update {
                    if (it.selectedExecutionId == executionId) {
                        it.copy(
                            selectedAnswers = answerItems,
                            isDetailLoading = false
                        )
                    } else {
                        it
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        selectedAnswers = emptyList(),
                        isDetailLoading = false,
                        message = context.getString(R.string.routine_history_message_answers_error)
                    )
                }
            }
        }
    }

    private fun loadHistory() {
        if (routineIdArg <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = context.getString(R.string.routine_history_message_invalid_routine)
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val routine = repository.getRoutineById(routineIdArg)
                if (routine == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = context.getString(R.string.routine_history_message_load_routine_error)
                        )
                    }
                    return@launch
                }

                val executions = repository.getCompletedExecutionsForRoutine(routineIdArg)
                _uiState.update {
                    it.copy(
                        routineName = routine.name,
                        executions = executions.map { execution -> execution.toHistoryItem() },
                        isLoading = false,
                        message = null
                    )
                }

                if (executionIdArg > 0L && executions.any { it.executionId == executionIdArg }) {
                    loadExecutionAnswers(executionIdArg)
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = context.getString(R.string.routine_history_message_load_error)
                    )
                }
            }
        }
    }
}

private fun RoutineExecution.toHistoryItem(): RoutineHistoryExecutionItem =
    RoutineHistoryExecutionItem(
        executionId = executionId,
        date = scheduledDate,
        status = status
    )

private fun RoutineQuestion.toHistoryAnswerItem(answer: RoutineAnswer?, context: Context): RoutineHistoryAnswerItem =
    RoutineHistoryAnswerItem(
        questionLabel = label,
        fieldType = fieldType,
        value = answer.toDisplayValue(this, context)
    )

private fun RoutineAnswer?.toDisplayValue(question: RoutineQuestion, context: Context): String {
    if (this == null) return context.getString(R.string.routine_history_no_answer)

    return when (question.fieldType) {
        QuestionFieldType.TEXT -> valueText?.takeIf { it.isNotBlank() } ?: context.getString(R.string.routine_history_no_answer)
        QuestionFieldType.NUMBER -> valueNumber?.toDisplayText() ?: context.getString(R.string.routine_history_no_answer)
        QuestionFieldType.BOOLEAN -> valueBoolean?.let { if (it) context.getString(R.string.common_yes) else context.getString(R.string.common_no) } ?: context.getString(R.string.routine_history_no_answer)
        QuestionFieldType.SINGLE_CHOICE -> {
            val optionId = valueText?.toLongOrNull() ?: return context.getString(R.string.routine_history_no_answer)
            question.options.firstOrNull { it.optionId == optionId }?.text
                ?: context.getString(R.string.routine_history_option_missing)
        }
        QuestionFieldType.MULTIPLE_CHOICE -> {
            val selectedOptionIds = valueJson.decodeSelectedOptionIds()
            if (selectedOptionIds.isEmpty()) {
                return context.getString(R.string.routine_history_no_answer)
            }

            val selectedOptions = question.options
                .filter { it.optionId in selectedOptionIds }
                .joinToString(separator = ", ") { it.text }
            selectedOptions.ifBlank { context.getString(R.string.routine_history_option_missing) }
        }
        QuestionFieldType.SCALE -> context.getString(R.string.routine_history_unsupported_type)
    }
}

private fun String?.decodeSelectedOptionIds(): Set<Long> {
    if (isNullOrBlank()) return emptySet()

    return trim()
        .removePrefix("[")
        .removeSuffix("]")
        .split(",")
        .mapNotNull { it.trim().toLongOrNull() }
        .toSet()
}

private fun Double.toDisplayText(): String =
    if (rem(1.0) == 0.0) toLong().toString() else toString()
