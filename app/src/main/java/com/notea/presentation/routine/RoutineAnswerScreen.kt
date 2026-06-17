package com.notea.presentation.routine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.domain.model.QuestionFieldType
import com.notea.domain.model.RoutineExecutionStatus
import java.time.format.DateTimeFormatter

@Composable
fun RoutineAnswerRoute(
    onBack: () -> Unit,
    viewModel: RoutineAnswerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageConsumed()
        }
    }

    RoutineAnswerScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTextChanged = viewModel::onTextChanged,
        onBooleanChanged = viewModel::onBooleanChanged,
        onSingleChoiceChanged = viewModel::onSingleChoiceChanged,
        onMultipleChoiceToggled = viewModel::onMultipleChoiceToggled,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineAnswerScreen(
    uiState: RoutineAnswerUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTextChanged: (Long, String) -> Unit,
    onBooleanChanged: (Long, Boolean) -> Unit,
    onSingleChoiceChanged: (Long, Long) -> Unit,
    onMultipleChoiceToggled: (Long, Long) -> Unit,
    onSave: () -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
    val isBusy = uiState.isLoading || uiState.isSaving

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.routine_answer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.routine_answer_loading))
            }

            uiState.blockingError?.let { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState.routineName.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.routineName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(stringResource(R.string.routine_answer_date, uiState.date?.format(formatter).orEmpty()))
                        Text(stringResource(R.string.routine_answer_status, stringResource(uiState.status.labelRes())))
                        if (uiState.status == RoutineExecutionStatus.ANSWERED) {
                            Text(stringResource(R.string.routine_answer_edit_saved))
                        }
                    }
                }
            }

            if (!uiState.isLoading && uiState.executionId != null && uiState.questions.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.routine_answer_no_questions_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(stringResource(R.string.routine_answer_no_questions_body))
                    }
                }
            }

            uiState.questions.forEachIndexed { index, question ->
                RoutineAnswerQuestionCard(
                    index = index,
                    question = question,
                    enabled = !isBusy,
                    onTextChanged = onTextChanged,
                    onBooleanChanged = onBooleanChanged,
                    onSingleChoiceChanged = onSingleChoiceChanged,
                    onMultipleChoiceToggled = onMultipleChoiceToggled
                )
            }

            if (uiState.questions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                        onClick = onBack
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = uiState.canSave,
                        onClick = onSave
                    ) {
                        Text(if (uiState.isSaving) stringResource(R.string.common_saving) else stringResource(R.string.common_save))
                    }
                }
            }

            if (uiState.isSaved) {
                Text(
                    text = stringResource(R.string.routine_answer_saved_inline),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun RoutineAnswerQuestionCard(
    index: Int,
    question: RoutineAnswerQuestionItem,
    enabled: Boolean,
    onTextChanged: (Long, String) -> Unit,
    onBooleanChanged: (Long, Boolean) -> Unit,
    onSingleChoiceChanged: (Long, Long) -> Unit,
    onMultipleChoiceToggled: (Long, Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.routine_answer_question_title, index + 1),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = if (question.isRequired) stringResource(R.string.routine_answer_required_suffix, question.label) else question.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.routine_answer_question_meta, stringResource(question.fieldType.labelRes()), stringResource(if (question.isRequired) R.string.routine_answer_required else R.string.routine_answer_optional)),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            when (question.fieldType) {
                QuestionFieldType.TEXT -> {
                    OutlinedTextField(
                        value = question.textValue,
                        onValueChange = { onTextChanged(question.questionId, it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        isError = question.error != null,
                        label = { Text(stringResource(R.string.common_answer)) },
                        supportingText = question.error?.let { error ->
                            { Text(error) }
                        }
                    )
                }
                QuestionFieldType.NUMBER -> {
                    OutlinedTextField(
                        value = question.textValue,
                        onValueChange = { onTextChanged(question.questionId, it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        isError = question.error != null,
                        label = { Text(stringResource(R.string.common_number)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = question.error?.let { error ->
                            { Text(error) }
                        }
                    )
                }
                QuestionFieldType.BOOLEAN -> {
                    BooleanAnswerOption(
                        label = stringResource(R.string.common_yes),
                        selected = question.booleanValue == true,
                        enabled = enabled,
                        onClick = { onBooleanChanged(question.questionId, true) }
                    )
                    BooleanAnswerOption(
                        label = stringResource(R.string.common_no),
                        selected = question.booleanValue == false,
                        enabled = enabled,
                        onClick = { onBooleanChanged(question.questionId, false) }
                    )
                    QuestionError(question.error)
                }
                QuestionFieldType.SINGLE_CHOICE -> {
                    question.options.forEach { option ->
                        val selected = question.selectedOptionId == option.optionId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (enabled) {
                                        Modifier.clickable {
                                            onSingleChoiceChanged(question.questionId, option.optionId)
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                enabled = enabled,
                                onClick = { onSingleChoiceChanged(question.questionId, option.optionId) }
                            )
                            Text(option.text)
                        }
                    }
                    QuestionError(question.error)
                }
                QuestionFieldType.MULTIPLE_CHOICE -> {
                    question.options.forEach { option ->
                        val selected = option.optionId in question.selectedOptionIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (enabled) {
                                        Modifier.clickable {
                                            onMultipleChoiceToggled(question.questionId, option.optionId)
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected,
                                enabled = enabled,
                                onCheckedChange = null
                            )
                            Text(option.text)
                        }
                    }
                    QuestionError(question.error)
                }
                else -> {
                    Text(
                        text = stringResource(R.string.routine_answer_unsupported_type),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BooleanAnswerOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = onClick
        )
        Text(label)
    }
}

@Composable
private fun QuestionError(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun RoutineExecutionStatus?.labelRes(): Int = when (this) {
    RoutineExecutionStatus.PENDING -> R.string.common_status_pending
    RoutineExecutionStatus.ANSWERED -> R.string.common_status_answered
    RoutineExecutionStatus.OMITTED -> R.string.common_status_omitted
    null -> R.string.common_status_unknown
}

private fun QuestionFieldType.labelRes(): Int = when (this) {
    QuestionFieldType.TEXT -> R.string.question_type_text
    QuestionFieldType.NUMBER -> R.string.question_type_number
    QuestionFieldType.BOOLEAN -> R.string.question_type_boolean
    QuestionFieldType.SINGLE_CHOICE -> R.string.question_type_single_choice
    QuestionFieldType.MULTIPLE_CHOICE -> R.string.question_type_multiple_choice
    QuestionFieldType.SCALE -> R.string.question_type_scale
}
