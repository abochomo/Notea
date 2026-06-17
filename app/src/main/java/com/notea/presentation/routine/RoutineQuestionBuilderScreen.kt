package com.notea.presentation.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.domain.model.QuestionFieldType

@Composable
fun RoutineQuestionBuilderRoute(
    onBack: () -> Unit,
    viewModel: RoutineQuestionBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageConsumed()
        }
    }

    RoutineQuestionBuilderScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddQuestion = viewModel::addQuestion,
        onQuestionLabelChanged = viewModel::onQuestionLabelChanged,
        onQuestionTypeChanged = viewModel::onQuestionTypeChanged,
        onRequiredChanged = viewModel::onRequiredChanged,
        onAddOption = viewModel::addOption,
        onOptionTextChanged = viewModel::onOptionTextChanged,
        onRemoveOption = viewModel::removeOption,
        onMoveQuestionUp = viewModel::moveQuestionUp,
        onMoveQuestionDown = viewModel::moveQuestionDown,
        onDeleteQuestionRequested = viewModel::requestDeleteQuestion,
        onDeleteQuestionDismissed = viewModel::dismissDeleteQuestion,
        onDeleteQuestionConfirmed = viewModel::confirmDeleteQuestion,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RoutineQuestionBuilderScreen(
    uiState: RoutineQuestionBuilderUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddQuestion: () -> Unit,
    onQuestionLabelChanged: (Long, String) -> Unit,
    onQuestionTypeChanged: (Long, QuestionFieldType) -> Unit,
    onRequiredChanged: (Long, Boolean) -> Unit,
    onAddOption: (Long) -> Unit,
    onOptionTextChanged: (Long, Long, String) -> Unit,
    onRemoveOption: (Long, Long) -> Unit,
    onMoveQuestionUp: (Long) -> Unit,
    onMoveQuestionDown: (Long) -> Unit,
    onDeleteQuestionRequested: (Long) -> Unit,
    onDeleteQuestionDismissed: () -> Unit,
    onDeleteQuestionConfirmed: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.routine_questions_title)) },
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
                Text(
                    text = uiState.routineName,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canEdit,
                onClick = onAddQuestion,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.routine_questions_add))
            }

            if (!uiState.isLoading && uiState.questions.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.routine_questions_empty_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(stringResource(R.string.routine_questions_empty_body))
                    }
                }
            }

            uiState.questions.forEachIndexed { index, question ->
                QuestionEditorCard(
                    index = index,
                    totalQuestions = uiState.questions.size,
                    question = question,
                    enabled = uiState.canEdit,
                    onQuestionLabelChanged = onQuestionLabelChanged,
                    onQuestionTypeChanged = onQuestionTypeChanged,
                    onRequiredChanged = onRequiredChanged,
                    onAddOption = onAddOption,
                    onOptionTextChanged = onOptionTextChanged,
                    onRemoveOption = onRemoveOption,
                    onMoveQuestionUp = onMoveQuestionUp,
                    onMoveQuestionDown = onMoveQuestionDown,
                    onDeleteQuestionRequested = onDeleteQuestionRequested
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving,
                    onClick = onBack,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) { Text(stringResource(R.string.common_cancel)) }

                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canEdit,
                    onClick = onSave,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(if (uiState.isSaving) stringResource(R.string.common_saving) else stringResource(R.string.routine_questions_save_form))
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (uiState.pendingDeleteQuestionId != null) {
        AlertDialog(
            onDismissRequest = onDeleteQuestionDismissed,
            title = { Text(stringResource(R.string.routine_questions_delete_title)) },
            text = { Text(stringResource(R.string.routine_questions_delete_body)) },
            confirmButton = {
                TextButton(onClick = onDeleteQuestionConfirmed) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteQuestionDismissed) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionEditorCard(
    index: Int,
    totalQuestions: Int,
    question: QuestionEditorItem,
    enabled: Boolean,
    onQuestionLabelChanged: (Long, String) -> Unit,
    onQuestionTypeChanged: (Long, QuestionFieldType) -> Unit,
    onRequiredChanged: (Long, Boolean) -> Unit,
    onAddOption: (Long) -> Unit,
    onOptionTextChanged: (Long, Long, String) -> Unit,
    onRemoveOption: (Long, Long) -> Unit,
    onMoveQuestionUp: (Long) -> Unit,
    onMoveQuestionDown: (Long) -> Unit,
    onDeleteQuestionRequested: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.routine_questions_question_title, index + 1),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.routine_questions_meta, stringResource(question.fieldType.labelRes()), stringResource(if (question.isRequired) R.string.routine_questions_required else R.string.routine_questions_optional)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(
                    enabled = enabled,
                    onClick = { onDeleteQuestionRequested(question.id) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.a11y_delete_question))
                }
            }

            OutlinedTextField(
                value = question.label,
                onValueChange = { onQuestionLabelChanged(question.id, it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                isError = question.labelError != null,
                label = { Text(stringResource(R.string.routine_questions_statement)) },
                supportingText = question.labelError?.let { error ->
                    { Text(error) }
                }
            )

            Text(stringResource(R.string.routine_questions_type))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedQuestionFieldTypes.forEach { type ->
                    FilterChip(
                        enabled = enabled,
                        selected = question.fieldType == type,
                        onClick = { onQuestionTypeChanged(question.id, type) },
                        label = { Text(stringResource(type.labelRes())) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.routine_questions_required))
                Switch(
                    enabled = enabled,
                    checked = question.isRequired,
                    onCheckedChange = { onRequiredChanged(question.id, it) }
                )
            }

            if (question.fieldType.hasOptions()) {
                Text(stringResource(R.string.routine_questions_options))
                question.options.forEachIndexed { optionIndex, option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = option.text,
                            onValueChange = { onOptionTextChanged(question.id, option.id, it) },
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            isError = option.error != null,
                            label = { Text(stringResource(R.string.routine_questions_option_label, optionIndex + 1)) },
                            supportingText = option.error?.let { error ->
                                { Text(error) }
                            }
                        )
                        IconButton(
                            enabled = enabled,
                            onClick = { onRemoveOption(question.id, option.id) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.a11y_delete_option))
                        }
                    }
                }
                if (question.optionsError != null) {
                    Text(
                        text = question.optionsError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(
                    enabled = enabled,
                    onClick = { onAddOption(question.id) }
                ) {
                    Text(stringResource(R.string.routine_questions_add_option))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = enabled && index > 0,
                    onClick = { onMoveQuestionUp(question.id) }
                ) {
                    Text(stringResource(R.string.routine_questions_move_up))
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = enabled && index < totalQuestions - 1,
                    onClick = { onMoveQuestionDown(question.id) }
                ) {
                    Text(stringResource(R.string.routine_questions_move_down))
                }
            }
        }
    }
}

private fun QuestionFieldType.labelRes(): Int = when (this) {
    QuestionFieldType.TEXT -> R.string.question_type_text
    QuestionFieldType.NUMBER -> R.string.question_type_number
    QuestionFieldType.BOOLEAN -> R.string.question_type_boolean
    QuestionFieldType.SINGLE_CHOICE -> R.string.question_type_single_choice
    QuestionFieldType.MULTIPLE_CHOICE -> R.string.question_type_multiple_choice
    QuestionFieldType.SCALE -> R.string.question_type_scale
}

private fun QuestionFieldType.hasOptions(): Boolean =
    this == QuestionFieldType.SINGLE_CHOICE || this == QuestionFieldType.MULTIPLE_CHOICE
