package com.notea.presentation.routine

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.R
import com.notea.domain.model.QuestionFieldType
import com.notea.domain.model.RoutineAnswer
import com.notea.domain.model.RoutineAnswerDraft
import com.notea.domain.model.RoutineExecutionStatus
import com.notea.domain.model.RoutineQuestion
import com.notea.domain.model.RoutineQuestionOption
import com.notea.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineAnswerUiState(
    val routineId: Long = -1L,
    val routineName: String = "",
    val date: LocalDate? = null,
    val executionId: Long? = null,
    val status: RoutineExecutionStatus? = null,
    val questions: List<RoutineAnswerQuestionItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val message: String? = null,
    val blockingError: String? = null
) {
    val canSave: Boolean
        get() = !isLoading && !isSaving && executionId != null && blockingError == null && questions.isNotEmpty()
}

data class RoutineAnswerQuestionItem(
    val questionId: Long,
    val label: String,
    val fieldType: QuestionFieldType,
    val isRequired: Boolean,
    val options: List<RoutineQuestionOption> = emptyList(),
    val textValue: String = "",
    val booleanValue: Boolean? = null,
    val selectedOptionId: Long? = null,
    val selectedOptionIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class RoutineAnswerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val repository: RoutineRepository
) : ViewModel() {

    private val routineIdArg: Long = savedStateHandle["routineId"] ?: -1L
    private val dateText: String? = savedStateHandle["date"]
    private val dateArg: LocalDate? = dateText
        ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

    private val _uiState = MutableStateFlow(
        RoutineAnswerUiState(
            routineId = routineIdArg,
            date = dateArg
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadForm()
    }

    fun onTextChanged(questionId: Long, value: String) {
        updateQuestion(questionId) {
            it.copy(textValue = value, error = null)
        }
    }

    fun onBooleanChanged(questionId: Long, value: Boolean) {
        updateQuestion(questionId) {
            it.copy(booleanValue = value, error = null)
        }
    }

    fun onSingleChoiceChanged(questionId: Long, optionId: Long) {
        updateQuestion(questionId) {
            it.copy(selectedOptionId = optionId, error = null)
        }
    }

    fun onMultipleChoiceToggled(questionId: Long, optionId: Long) {
        updateQuestion(questionId) { question ->
            val selectedOptionIds = if (optionId in question.selectedOptionIds) {
                question.selectedOptionIds - optionId
            } else {
                question.selectedOptionIds + optionId
            }
            question.copy(selectedOptionIds = selectedOptionIds, error = null)
        }
    }

    fun save() {
        val current = _uiState.value
        val executionId = current.executionId ?: return
        if (current.isLoading || current.isSaving || current.blockingError != null) return

        val validatedQuestions = current.questions.validateAnswers(context)
        if (validatedQuestions.any { it.error != null }) {
            _uiState.update {
                it.copy(
                    questions = validatedQuestions,
                    message = context.getString(R.string.routine_answer_message_review)
                )
            }
            return
        }

        _uiState.update {
            it.copy(isSaving = true, isSaved = false, message = null)
        }
        viewModelScope.launch {
            try {
                val execution = repository.saveExecutionAnswers(
                    executionId = executionId,
                    answers = validatedQuestions.mapNotNull { it.toDraftOrNull() }
                )
                if (execution == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            message = context.getString(R.string.routine_answer_message_save_execution_error)
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        status = execution.status,
                        isSaving = false,
                        isSaved = true,
                        message = context.getString(R.string.routine_answer_message_saved)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = context.getString(R.string.routine_answer_message_save_error)
                    )
                }
            }
        }
    }

    fun onMessageConsumed() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loadForm() {
        val date = dateArg
        if (routineIdArg <= 0L || date == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    blockingError = context.getString(R.string.routine_answer_error_invalid_route)
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
                            blockingError = context.getString(R.string.routine_answer_error_load_routine)
                        )
                    }
                    return@launch
                }

                val execution = repository.getOrCreateExecution(routineIdArg, date)
                if (execution == null) {
                    _uiState.update {
                        it.copy(
                            routineName = routine.name,
                            isLoading = false,
                            blockingError = context.getString(R.string.routine_answer_error_wrong_date)
                        )
                    }
                    return@launch
                }

                val questions = repository.getQuestionsForRoutine(routineIdArg)
                val answersByQuestion = repository.getAnswersForExecution(execution.executionId)
                    .associateBy { it.questionId }

                _uiState.update {
                    it.copy(
                        routineName = routine.name,
                        executionId = execution.executionId,
                        status = execution.status,
                        questions = questions.map { question ->
                            question.toAnswerItem(answersByQuestion[question.questionId])
                        },
                        isLoading = false,
                        blockingError = null
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        blockingError = context.getString(R.string.routine_answer_error_prepare)
                    )
                }
            }
        }
    }

    private fun updateQuestion(
        questionId: Long,
        transform: (RoutineAnswerQuestionItem) -> RoutineAnswerQuestionItem
    ) {
        val current = _uiState.value
        if (current.isLoading || current.isSaving) return

        _uiState.update {
            it.copy(
                questions = it.questions.map { question ->
                    if (question.questionId == questionId) transform(question) else question
                },
                isSaved = false
            )
        }
    }
}

private fun RoutineQuestion.toAnswerItem(answer: RoutineAnswer?): RoutineAnswerQuestionItem {
    val fieldType = fieldType.takeIf { it in supportedAnswerFieldTypes } ?: QuestionFieldType.TEXT
    return RoutineAnswerQuestionItem(
        questionId = questionId,
        label = label,
        fieldType = fieldType,
        isRequired = isRequired,
        options = if (fieldType.hasOptions()) options else emptyList(),
        textValue = when (fieldType) {
            QuestionFieldType.TEXT -> answer?.valueText.orEmpty()
            QuestionFieldType.NUMBER -> answer?.valueNumber?.toDisplayText().orEmpty()
            else -> ""
        },
        booleanValue = if (fieldType == QuestionFieldType.BOOLEAN) answer?.valueBoolean else null,
        selectedOptionId = if (fieldType == QuestionFieldType.SINGLE_CHOICE) {
            answer?.valueText?.toLongOrNull()
        } else {
            null
        },
        selectedOptionIds = if (fieldType == QuestionFieldType.MULTIPLE_CHOICE) {
            answer?.valueJson.decodeSelectedOptionIds(options)
        } else {
            emptySet()
        }
    )
}

private val supportedAnswerFieldTypes = setOf(
    QuestionFieldType.TEXT,
    QuestionFieldType.NUMBER,
    QuestionFieldType.BOOLEAN,
    QuestionFieldType.SINGLE_CHOICE,
    QuestionFieldType.MULTIPLE_CHOICE
)

private fun List<RoutineAnswerQuestionItem>.validateAnswers(context: Context): List<RoutineAnswerQuestionItem> =
    map { question ->
        val error = when (question.fieldType) {
            QuestionFieldType.TEXT -> {
                if (question.isRequired && question.textValue.isBlank()) {
                    context.getString(R.string.routine_answer_error_required)
                } else {
                    null
                }
            }
            QuestionFieldType.NUMBER -> {
                val value = question.textValue.trim()
                when {
                    value.isBlank() && question.isRequired -> context.getString(R.string.routine_answer_error_required)
                    value.isNotBlank() && value.toAnswerNumberOrNull() == null -> context.getString(R.string.routine_answer_error_invalid_number)
                    else -> null
                }
            }
            QuestionFieldType.BOOLEAN -> {
                if (question.isRequired && question.booleanValue == null) {
                    context.getString(R.string.routine_answer_error_boolean_required)
                } else {
                    null
                }
            }
            QuestionFieldType.SINGLE_CHOICE -> {
                if (question.isRequired && question.selectedOptionId == null) {
                    context.getString(R.string.routine_answer_error_option_required)
                } else {
                    null
                }
            }
            QuestionFieldType.MULTIPLE_CHOICE -> {
                if (question.isRequired && question.selectedOptionIds.isEmpty()) {
                    context.getString(R.string.routine_answer_error_multiple_option_required)
                } else {
                    null
                }
            }
            else -> context.getString(R.string.routine_answer_error_unsupported_type)
        }
        question.copy(error = error)
    }

private fun RoutineAnswerQuestionItem.toDraftOrNull(): RoutineAnswerDraft? =
    when (fieldType) {
        QuestionFieldType.TEXT -> textValue.trim()
            .takeIf { it.isNotBlank() }
            ?.let { RoutineAnswerDraft(questionId = questionId, valueText = it) }
        QuestionFieldType.NUMBER -> textValue.trim()
            .takeIf { it.isNotBlank() }
            ?.toAnswerNumberOrNull()
            ?.let { RoutineAnswerDraft(questionId = questionId, valueNumber = it) }
        QuestionFieldType.BOOLEAN -> booleanValue
            ?.let { RoutineAnswerDraft(questionId = questionId, valueBoolean = it) }
        QuestionFieldType.SINGLE_CHOICE -> selectedOptionId
            ?.let { RoutineAnswerDraft(questionId = questionId, valueText = it.toString()) }
        QuestionFieldType.MULTIPLE_CHOICE -> selectedOptionIds
            .takeIf { it.isNotEmpty() }
            ?.let { RoutineAnswerDraft(questionId = questionId, valueJson = it.toJsonArray()) }
        else -> null
    }

private fun QuestionFieldType.hasOptions(): Boolean =
    this == QuestionFieldType.SINGLE_CHOICE || this == QuestionFieldType.MULTIPLE_CHOICE

private fun String?.decodeSelectedOptionIds(options: List<RoutineQuestionOption>): Set<Long> {
    if (isNullOrBlank()) return emptySet()

    val validOptionIds = options.map { it.optionId }.toSet()
    return trim()
        .removePrefix("[")
        .removeSuffix("]")
        .split(",")
        .mapNotNull { it.trim().toLongOrNull() }
        .filter { it in validOptionIds }
        .toSet()
}

private fun Set<Long>.toJsonArray(): String =
    sorted().joinToString(prefix = "[", postfix = "]", separator = ",")

private fun String.toAnswerNumberOrNull(): Double? =
    replace(',', '.').toDoubleOrNull()

private fun Double.toDisplayText(): String =
    if (rem(1.0) == 0.0) toLong().toString() else toString()
