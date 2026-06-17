package com.notea.presentation.routine

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notea.R
import com.notea.domain.model.QuestionFieldType
import com.notea.domain.model.RoutineQuestion
import com.notea.domain.model.RoutineQuestionDraft
import com.notea.domain.model.RoutineQuestionOptionDraft
import com.notea.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineQuestionBuilderUiState(
    val routineId: Long = -1L,
    val routineName: String = "",
    val questions: List<QuestionEditorItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val pendingDeleteQuestionId: Long? = null,
    val blockingError: String? = null,
    val message: String? = null
) {
    val canEdit: Boolean get() = !isLoading && !isSaving && routineId > 0L && blockingError == null
}

data class QuestionEditorItem(
    val id: Long,
    val questionId: Long?,
    val label: String,
    val fieldType: QuestionFieldType,
    val isRequired: Boolean,
    val options: List<QuestionOptionEditorItem> = emptyList(),
    val labelError: String? = null,
    val optionsError: String? = null
)

data class QuestionOptionEditorItem(
    val id: Long,
    val optionId: Long?,
    val text: String,
    val error: String? = null
)

val supportedQuestionFieldTypes = listOf(
    QuestionFieldType.TEXT,
    QuestionFieldType.NUMBER,
    QuestionFieldType.BOOLEAN,
    QuestionFieldType.SINGLE_CHOICE,
    QuestionFieldType.MULTIPLE_CHOICE
)

@HiltViewModel
class RoutineQuestionBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val repository: RoutineRepository
) : ViewModel() {

    private val routineIdArg: Long = savedStateHandle["routineId"] ?: -1L
    private var nextLocalId = -1L

    private val _uiState = MutableStateFlow(
        RoutineQuestionBuilderUiState(routineId = routineIdArg)
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadQuestions()
    }

    fun addQuestion() {
        if (!_uiState.value.canEdit) return

        _uiState.update {
            it.copy(
                questions = it.questions + QuestionEditorItem(
                    id = newLocalId(),
                    questionId = null,
                    label = "",
                    fieldType = QuestionFieldType.TEXT,
                    isRequired = false
                )
            )
        }
    }

    fun onQuestionLabelChanged(questionId: Long, value: String) {
        updateQuestion(questionId) {
            it.copy(label = value, labelError = null)
        }
    }

    fun onQuestionTypeChanged(questionId: Long, value: QuestionFieldType) {
        if (value !in supportedQuestionFieldTypes) return

        updateQuestion(questionId) { question ->
            val options = if (value.hasOptions()) {
                question.options.ifEmpty {
                    listOf(
                        QuestionOptionEditorItem(id = newLocalId(), optionId = null, text = ""),
                        QuestionOptionEditorItem(id = newLocalId(), optionId = null, text = "")
                    )
                }
            } else {
                question.options
            }

            question.copy(
                fieldType = value,
                options = options,
                optionsError = null
            )
        }
    }

    fun onRequiredChanged(questionId: Long, value: Boolean) {
        updateQuestion(questionId) { it.copy(isRequired = value) }
    }

    fun addOption(questionId: Long) {
        updateQuestion(questionId) {
            if (!it.fieldType.hasOptions()) {
                it
            } else {
                it.copy(
                    options = it.options + QuestionOptionEditorItem(
                        id = newLocalId(),
                        optionId = null,
                        text = ""
                    ),
                    optionsError = null
                )
            }
        }
    }

    fun onOptionTextChanged(questionId: Long, optionId: Long, value: String) {
        updateQuestion(questionId) { question ->
            question.copy(
                options = question.options.map { option ->
                    if (option.id == optionId) {
                        option.copy(text = value, error = null)
                    } else {
                        option
                    }
                },
                optionsError = null
            )
        }
    }

    fun removeOption(questionId: Long, optionId: Long) {
        updateQuestion(questionId) { question ->
            question.copy(
                options = question.options.filterNot { it.id == optionId },
                optionsError = null
            )
        }
    }

    fun moveQuestionUp(questionId: Long) {
        val current = _uiState.value
        if (!current.canEdit) return

        val index = current.questions.indexOfFirst { it.id == questionId }
        if (index <= 0) return

        _uiState.update {
            it.copy(questions = it.questions.swap(index, index - 1))
        }
    }

    fun moveQuestionDown(questionId: Long) {
        val current = _uiState.value
        if (!current.canEdit) return

        val index = current.questions.indexOfFirst { it.id == questionId }
        if (index == -1 || index >= current.questions.lastIndex) return

        _uiState.update {
            it.copy(questions = it.questions.swap(index, index + 1))
        }
    }

    fun requestDeleteQuestion(questionId: Long) {
        if (!_uiState.value.canEdit) return
        _uiState.update { it.copy(pendingDeleteQuestionId = questionId) }
    }

    fun dismissDeleteQuestion() {
        _uiState.update { it.copy(pendingDeleteQuestionId = null) }
    }

    fun confirmDeleteQuestion() {
        val pendingId = _uiState.value.pendingDeleteQuestionId ?: return
        _uiState.update {
            it.copy(
                questions = it.questions.filterNot { question -> question.id == pendingId },
                pendingDeleteQuestionId = null
            )
        }
    }

    fun save() {
        val current = _uiState.value
        if (!current.canEdit) return

        val validatedQuestions = current.questions.validateQuestions(context)
        if (validatedQuestions.any { it.hasErrors() }) {
            _uiState.update {
                it.copy(
                    questions = validatedQuestions,
                    message = context.getString(R.string.routine_questions_message_review)
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            try {
                if (repository.getRoutineById(current.routineId) == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            blockingError = context.getString(R.string.routine_questions_error_missing_routine),
                            message = context.getString(R.string.routine_questions_error_missing_routine)
                        )
                    }
                    return@launch
                }

                repository.replaceQuestionsForRoutine(
                    routineId = current.routineId,
                    questions = validatedQuestions.mapIndexed { index, question ->
                        question.toDraft(current.routineId, index)
                    }
                )
                val savedQuestions = repository.getQuestionsForRoutine(current.routineId)
                _uiState.update {
                    it.copy(
                        questions = savedQuestions.map(::toEditorItem),
                        isSaving = false,
                        message = context.getString(R.string.routine_questions_message_saved)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = context.getString(R.string.routine_questions_message_save_error)
                    )
                }
            }
        }
    }

    fun onMessageConsumed() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loadQuestions() {
        if (routineIdArg <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    blockingError = context.getString(R.string.routine_questions_error_invalid_routine),
                    message = context.getString(R.string.routine_questions_error_invalid_routine)
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
                            blockingError = context.getString(R.string.routine_questions_error_missing_routine),
                            message = context.getString(R.string.routine_questions_error_load_routine)
                        )
                    }
                    return@launch
                }

                val questions = repository.getQuestionsForRoutine(routineIdArg)
                _uiState.update {
                    it.copy(
                        routineName = routine.name,
                        questions = questions.map(::toEditorItem),
                        isLoading = false,
                        blockingError = null
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        blockingError = context.getString(R.string.routine_questions_error_load_questions),
                        message = context.getString(R.string.routine_questions_error_load_questions)
                    )
                }
            }
        }
    }

    private fun updateQuestion(
        questionId: Long,
        transform: (QuestionEditorItem) -> QuestionEditorItem
    ) {
        if (!_uiState.value.canEdit) return

        _uiState.update {
            it.copy(
                questions = it.questions.map { question ->
                    if (question.id == questionId) transform(question) else question
                }
            )
        }
    }

    private fun toEditorItem(question: RoutineQuestion): QuestionEditorItem {
        val fieldType = question.fieldType.takeIf { it in supportedQuestionFieldTypes }
            ?: QuestionFieldType.TEXT
        return QuestionEditorItem(
            id = question.questionId,
            questionId = question.questionId,
            label = question.label,
            fieldType = fieldType,
            isRequired = question.isRequired,
            options = if (fieldType.hasOptions()) {
                question.options.map {
                    QuestionOptionEditorItem(
                        id = it.optionId,
                        optionId = it.optionId,
                        text = it.text
                    )
                }
            } else {
                emptyList()
            }
        )
    }

    private fun newLocalId(): Long = nextLocalId--
}

private fun QuestionEditorItem.toDraft(routineId: Long, position: Int): RoutineQuestionDraft =
    RoutineQuestionDraft(
        questionId = questionId,
        routineId = routineId,
        label = label.trim(),
        fieldType = fieldType,
        isRequired = isRequired,
        position = position,
        options = if (fieldType.hasOptions()) {
            options
                .map { it.text.trim() }
                .filter { it.isNotBlank() }
                .mapIndexed { index, text ->
                    RoutineQuestionOptionDraft(
                        optionId = null,
                        text = text,
                        position = index
                    )
                }
        } else {
            emptyList()
        }
    )

private fun List<QuestionEditorItem>.validateQuestions(context: Context): List<QuestionEditorItem> =
    map { question ->
        val labelError = if (question.label.isBlank()) context.getString(R.string.routine_questions_error_statement_required) else null
        val validatedOptions = if (question.fieldType.hasOptions()) {
            question.options.map { option ->
                option.copy(error = if (option.text.isBlank()) context.getString(R.string.routine_questions_error_option_required) else null)
            }
        } else {
            question.options
        }
        val validOptionCount = validatedOptions.count { it.text.isNotBlank() }
        val optionsError = if (
            question.fieldType.hasOptions() && validOptionCount < 2
        ) {
            context.getString(R.string.routine_questions_error_min_options)
        } else {
            null
        }

        question.copy(
            labelError = labelError,
            options = validatedOptions,
            optionsError = optionsError
        )
    }

private fun QuestionEditorItem.hasErrors(): Boolean =
    labelError != null || optionsError != null || options.any { it.error != null }

private fun QuestionFieldType.hasOptions(): Boolean =
    this == QuestionFieldType.SINGLE_CHOICE || this == QuestionFieldType.MULTIPLE_CHOICE

private fun <T> List<T>.swap(firstIndex: Int, secondIndex: Int): List<T> =
    toMutableList().also {
        val first = it[firstIndex]
        it[firstIndex] = it[secondIndex]
        it[secondIndex] = first
    }
