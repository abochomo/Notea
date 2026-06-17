package com.notea.presentation.reminder.editor

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.data.notification.NotificationChannels
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState

@Composable
fun ReminderEditorRoute(
    onBack: () -> Unit,
    viewModel: ReminderEditorViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            val shouldGoBack = uiState.closeAfterMessage
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageConsumed()
            if (shouldGoBack) onBack()
        }
    }

    ReminderEditorScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTitleChanged = viewModel::onTitleChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onDateSelected = viewModel::onDateSelected,
        onTimeSelected = viewModel::onTimeSelected,
        onNotificationsChanged = viewModel::onNotificationsChanged,
        onDeleteRequested = viewModel::requestDelete,
        onDeleteDismissed = viewModel::dismissDeleteConfirmation,
        onDeleteConfirmed = viewModel::delete,
        onCompleted = viewModel::markCompleted,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorScreen(
    uiState: ReminderEditorUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onCompleted: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var notificationStatus by remember { mutableStateOf(readReminderNotificationStatus(context)) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationStatus = readReminderNotificationStatus(context)
        permissionMessage = if (granted) {
            context.getString(R.string.reminder_notification_permission_granted)
        } else {
            context.getString(R.string.reminder_notification_permission_denied)
        }
    }

    LaunchedEffect(permissionMessage) {
        permissionMessage?.let {
            snackbarHostState.showSnackbar(it)
            permissionMessage = null
        }
    }

    LaunchedEffect(uiState.notificationsEnabled) {
        notificationStatus = readReminderNotificationStatus(context)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (uiState.reminderId == null) stringResource(R.string.reminder_editor_new_title) else stringResource(R.string.reminder_editor_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState.reminderId != null) {
                        IconButton(
                            enabled = uiState.canDelete,
                            onClick = onDeleteRequested
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.a11y_delete_reminder))
                        }
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
                value = uiState.title,
                onValueChange = onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isBusy && !uiState.isCompleted,
                label = { Text(stringResource(R.string.common_title)) }
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isBusy && !uiState.isCompleted,
                label = { Text(stringResource(R.string.common_description)) }
            )

            Text(stringResource(R.string.common_date))
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isBusy && !uiState.isCompleted,
                onClick = { showDatePicker = true },
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(uiState.selectedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: stringResource(R.string.reminder_select_date))
            }

            Text(stringResource(R.string.common_time))
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isBusy && !uiState.isCompleted,
                onClick = { showTimePicker = true },
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(uiState.selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: stringResource(R.string.reminder_select_time))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.reminder_notifications_active))
                Switch(
                    enabled = !uiState.isBusy && !uiState.isCompleted,
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = onNotificationsChanged
                )
            }

            if (uiState.notificationsEnabled) {
                ReminderNotificationStatusCard(
                    status = notificationStatus,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            if (uiState.isCompleted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.reminder_completed_card),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (uiState.reminderId != null) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canComplete,
                    onClick = onCompleted,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text(if (uiState.isCompleting) stringResource(R.string.reminder_marking_completed) else stringResource(R.string.reminder_mark_completed))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isBusy,
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
                            uiState.isDeleting -> stringResource(R.string.common_deleting)
                            uiState.isCompleting -> stringResource(R.string.reminder_marking_completed)
                            else -> stringResource(R.string.common_save)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate?.toDatePickerMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(it.toDatePickerLocalDate())
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.common_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val selectedTime = uiState.selectedTime ?: LocalTime.now().withSecond(0).withNano(0)
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_select_time_title)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.common_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text(stringResource(R.string.reminder_delete_dialog_title)) },
            text = { Text(stringResource(R.string.reminder_delete_dialog_body)) },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isDeleting,
                    onClick = onDeleteConfirmed
                ) {
                    Text(if (uiState.isDeleting) stringResource(R.string.common_deleting) else stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isDeleting,
                    onClick = onDeleteDismissed
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ReminderNotificationStatusCard(
    status: ReminderNotificationStatus,
    onRequestPermission: () -> Unit
) {
    if (status == ReminderNotificationStatus.Available) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_notification_status_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = when (status) {
                    ReminderNotificationStatus.RuntimePermissionRequired ->
                        stringResource(R.string.reminder_notification_runtime_required)
                    ReminderNotificationStatus.AppNotificationsDisabled ->
                        stringResource(R.string.reminder_notification_app_disabled)
                    ReminderNotificationStatus.ReminderChannelDisabled ->
                        stringResource(R.string.reminder_notification_channel_disabled)
                    ReminderNotificationStatus.Available -> ""
                }
            )
            if (status == ReminderNotificationStatus.RuntimePermissionRequired) {
                TextButton(onClick = onRequestPermission) {
                    Text(stringResource(R.string.reminder_allow_notifications))
                }
            }
        }
    }
}

private enum class ReminderNotificationStatus {
    Available,
    RuntimePermissionRequired,
    AppNotificationsDisabled,
    ReminderChannelDisabled
}

private fun readReminderNotificationStatus(context: Context): ReminderNotificationStatus {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return ReminderNotificationStatus.RuntimePermissionRequired
    }

    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        return ReminderNotificationStatus.AppNotificationsDisabled
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val reminderChannel = notificationManager.getNotificationChannel(NotificationChannels.REMINDERS)
        if (reminderChannel?.importance == NotificationManager.IMPORTANCE_NONE) {
            return ReminderNotificationStatus.ReminderChannelDisabled
        }
    }

    return ReminderNotificationStatus.Available
}

private fun LocalDate.toDatePickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toDatePickerLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
