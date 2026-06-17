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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.domain.model.FrequencyType
import java.time.LocalTime

@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    onOpenQuestionBuilder: (Long) -> Unit,
    onOpenHistory: (Long) -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            val shouldGoBack = uiState.isMissing
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageConsumed()
            if (shouldGoBack) onBack()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    RoutineEditorScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNameChanged = viewModel::onNameChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onFrequencyChanged = viewModel::onFrequencyChanged,
        onWeekdayToggled = viewModel::onWeekdayToggled,
        onAddTime = viewModel::addTime,
        onEditTime = viewModel::editTime,
        onRemoveTime = viewModel::removeTime,
        onOpenQuestionBuilder = onOpenQuestionBuilder,
        onOpenHistory = onOpenHistory,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RoutineEditorScreenContent(
    uiState: RoutineEditorUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onFrequencyChanged: (FrequencyType) -> Unit,
    onWeekdayToggled: (Int) -> Unit,
    onAddTime: (Int, Int) -> Unit,
    onEditTime: (String, Int, Int) -> Unit,
    onRemoveTime: (String) -> Unit,
    onOpenQuestionBuilder: (Long) -> Unit,
    onOpenHistory: (Long) -> Unit,
    onSave: () -> Unit
) {
    var showAddTimePicker by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<String?>(null) }
    val isBusy = uiState.isLoading || uiState.isSaving
    val canEditRoutine = !isBusy && !uiState.isMissing
    val canOpenRoutineTools = canEditRoutine

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (uiState.routineId == null) stringResource(R.string.routine_editor_new_title) else stringResource(R.string.routine_editor_edit_title)) },
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

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = canEditRoutine,
                label = { Text(stringResource(R.string.routine_name)) }
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = canEditRoutine,
                label = { Text(stringResource(R.string.common_description)) }
            )

            Text(stringResource(R.string.routine_frequency))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                supportedRoutineFrequencies.forEach { frequency ->
                    FilterChip(
                        enabled = canEditRoutine,
                        selected = uiState.frequencyType == frequency,
                        onClick = { onFrequencyChanged(frequency) },
                        label = { Text(stringResource(frequency.labelRes())) }
                    )
                }
            }

            if (uiState.frequencyType == FrequencyType.WEEKLY) {
                Text(stringResource(R.string.routine_weekdays))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    weekdays.forEach { day ->
                        FilterChip(
                            enabled = canEditRoutine,
                            selected = day.value in uiState.selectedWeekdays,
                            onClick = { onWeekdayToggled(day.value) },
                            label = { Text(stringResource(day.labelRes)) }
                        )
                    }
                }
            }

            Text(stringResource(R.string.routine_reminder_times))
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = canEditRoutine,
                onClick = { showAddTimePicker = true },
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.routine_add_time))
            }

            if (uiState.times.isEmpty()) {
                Text(
                    text = stringResource(R.string.routine_time_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.times.forEach { time ->
                        RoutineTimeItem(
                            time = time,
                            enabled = canEditRoutine,
                            onEdit = { editingTime = time },
                            onRemove = { onRemoveTime(time) }
                        )
                    }
                }
            }

            Text(stringResource(R.string.routine_questions))
            if (uiState.routineId == null) {
                Text(
                    text = stringResource(R.string.routine_save_before_questions),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canOpenRoutineTools,
                    onClick = { onOpenQuestionBuilder(uiState.routineId) },
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.routine_configure_questions))
                }

                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canOpenRoutineTools,
                    onClick = { onOpenHistory(uiState.routineId) },
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.routine_view_history))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy,
                    onClick = onBack,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) { Text(stringResource(R.string.common_cancel)) }

                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canSave && !uiState.isSaving,
                    onClick = onSave,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        when {
                            uiState.isLoading -> stringResource(R.string.common_loading)
                            uiState.isSaving -> stringResource(R.string.common_saving)
                            else -> stringResource(R.string.common_save)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showAddTimePicker) {
        RoutineTimePickerDialog(
            title = stringResource(R.string.routine_add_time),
            initialTime = LocalTime.of(8, 0),
            onDismiss = { showAddTimePicker = false },
            onConfirm = { selectedTime ->
                onAddTime(selectedTime.hour, selectedTime.minute)
                showAddTimePicker = false
            }
        )
    }

    editingTime?.let { currentTime ->
        RoutineTimePickerDialog(
            title = stringResource(R.string.routine_edit_time),
            initialTime = currentTime.toRoutineLocalTime(),
            onDismiss = { editingTime = null },
            onConfirm = { selectedTime ->
                onEditTime(currentTime, selectedTime.hour, selectedTime.minute)
                editingTime = null
            }
        )
    }
}

@Composable
private fun RoutineTimeItem(
    time: String,
    enabled: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = time,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(
                enabled = enabled,
                onClick = onEdit
            ) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.a11y_edit_time))
            }
            IconButton(
                enabled = enabled,
                onClick = onRemove
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.a11y_delete_time))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineTimePickerDialog(
    title: String,
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                }
            ) {
                Text(stringResource(R.string.common_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinePlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.routine_feature_unavailable_title))
            Text(stringResource(R.string.routine_feature_unavailable_message))
        }
    }
}

private data class WeekdayOption(
    val value: Int,
    val labelRes: Int
)

private val weekdays = listOf(
    WeekdayOption(1, R.string.weekday_monday),
    WeekdayOption(2, R.string.weekday_tuesday),
    WeekdayOption(3, R.string.weekday_wednesday),
    WeekdayOption(4, R.string.weekday_thursday),
    WeekdayOption(5, R.string.weekday_friday),
    WeekdayOption(6, R.string.weekday_saturday),
    WeekdayOption(7, R.string.weekday_sunday)
)

private fun FrequencyType.labelRes(): Int = when (this) {
    FrequencyType.DAILY -> R.string.routine_frequency_daily
    FrequencyType.WEEKLY -> R.string.routine_frequency_weekly
    FrequencyType.MONTHLY -> R.string.routine_frequency_monthly
    FrequencyType.CUSTOM -> R.string.routine_frequency_custom
}
private fun String.toRoutineLocalTime(): LocalTime {
    val parts = split(":")
    if (parts.size != 2) return LocalTime.of(8, 0)

    val hour = parts[0].toIntOrNull() ?: return LocalTime.of(8, 0)
    val minute = parts[1].toIntOrNull() ?: return LocalTime.of(8, 0)

    return if (hour in 0..23 && minute in 0..59) {
        LocalTime.of(hour, minute)
    } else {
        LocalTime.of(8, 0)
    }
}
