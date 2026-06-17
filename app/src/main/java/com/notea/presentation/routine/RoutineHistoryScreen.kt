package com.notea.presentation.routine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.domain.model.QuestionFieldType
import com.notea.domain.model.RoutineExecutionStatus
import java.time.format.DateTimeFormatter

@Composable
fun RoutineHistoryRoute(
    onBack: () -> Unit,
    viewModel: RoutineHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RoutineHistoryScreen(
        uiState = uiState,
        onBack = onBack,
        onExecutionSelected = viewModel::onExecutionSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineHistoryScreen(
    uiState: RoutineHistoryUiState,
    onBack: () -> Unit,
    onExecutionSelected: (Long) -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.routine_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.routine_history_loading),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            uiState.message?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (uiState.routineName.isNotBlank()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = uiState.routineName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(stringResource(R.string.routine_history_answered_executions))
                        }
                    }
                }
            }

            if (!uiState.isLoading && uiState.executions.isEmpty() && uiState.message == null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.routine_history_empty_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(stringResource(R.string.routine_history_empty_body))
                        }
                    }
                }
            }

            items(uiState.executions, key = { it.executionId }) { execution ->
                RoutineHistoryExecutionCard(
                    execution = execution,
                    isSelected = uiState.selectedExecutionId == execution.executionId,
                    isDetailLoading = uiState.isDetailLoading &&
                        uiState.selectedExecutionId == execution.executionId,
                    answers = if (uiState.selectedExecutionId == execution.executionId) {
                        uiState.selectedAnswers
                    } else {
                        emptyList()
                    },
                    dateFormatter = dateFormatter,
                    onClick = { onExecutionSelected(execution.executionId) }
                )
            }
        }
    }
}

@Composable
private fun RoutineHistoryExecutionCard(
    execution: RoutineHistoryExecutionItem,
    isSelected: Boolean,
    isDetailLoading: Boolean,
    answers: List<RoutineHistoryAnswerItem>,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = execution.date.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.routine_history_status, stringResource(execution.status.labelRes())))

            if (isSelected) {
                HorizontalDivider()
                if (isDetailLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.routine_history_loading_answers))
                } else if (answers.isEmpty()) {
                    Text(stringResource(R.string.routine_history_no_answers))
                } else {
                    answers.forEach { answer ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = answer.questionLabel,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.routine_history_answer_value, stringResource(answer.fieldType.labelRes()), answer.value),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.routine_history_tap_answers),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun RoutineExecutionStatus.labelRes(): Int = when (this) {
    RoutineExecutionStatus.PENDING -> R.string.common_status_pending
    RoutineExecutionStatus.ANSWERED -> R.string.common_status_answered
    RoutineExecutionStatus.OMITTED -> R.string.common_status_omitted
}

private fun QuestionFieldType.labelRes(): Int = when (this) {
    QuestionFieldType.TEXT -> R.string.question_type_text
    QuestionFieldType.NUMBER -> R.string.question_type_number
    QuestionFieldType.BOOLEAN -> R.string.question_type_boolean
    QuestionFieldType.SINGLE_CHOICE -> R.string.question_type_single_choice
    QuestionFieldType.MULTIPLE_CHOICE -> R.string.question_type_multiple_choice
    QuestionFieldType.SCALE -> R.string.question_type_scale
}
