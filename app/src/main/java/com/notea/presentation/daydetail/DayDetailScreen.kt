package com.notea.presentation.daydetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.domain.model.DayActivityItem
import com.notea.domain.model.RecentItemType
import com.notea.domain.model.RoutineExecutionStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayDetailRoute(
    date: LocalDate,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenReminder: (Long) -> Unit,
    onOpenRoutineAnswer: (Long, LocalDate) -> Unit,
    onOpenRoutineHistory: (Long, Long) -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel()
) {
    val items = viewModel.uiState.collectAsStateWithLifecycle().value
    DayDetailScreen(
        date = date,
        items = items,
        onBack = onBack,
        onOpenNote = onOpenNote,
        onOpenReminder = onOpenReminder,
        onOpenRoutineAnswer = onOpenRoutineAnswer,
        onOpenRoutineHistory = onOpenRoutineHistory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    items: List<DayActivityItem>,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenReminder: (Long) -> Unit,
    onOpenRoutineAnswer: (Long, LocalDate) -> Unit,
    onOpenRoutineHistory: (Long, Long) -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"))
    }
    val notes = items.filter { it.type == RecentItemType.NOTE }
    val reminders = items.filter { it.type == RecentItemType.REMINDER }
    val pendingRoutines = items.filter {
        it.type == RecentItemType.ROUTINE && it.routineStatus != RoutineExecutionStatus.ANSWERED
    }
    val completedRoutines = items.filter {
        it.type == RecentItemType.ROUTINE && it.routineStatus == RoutineExecutionStatus.ANSWERED
    }
    val notesTitle = stringResource(R.string.day_detail_notes_title)
    val notesEmpty = stringResource(R.string.day_detail_notes_empty)
    val remindersTitle = stringResource(R.string.day_detail_reminders_title)
    val remindersEmpty = stringResource(R.string.day_detail_reminders_empty)
    val pendingRoutinesTitle = stringResource(R.string.day_detail_pending_routines_title)
    val pendingRoutinesEmpty = stringResource(R.string.day_detail_pending_routines_empty)
    val completedRoutinesTitle = stringResource(R.string.day_detail_completed_routines_title)
    val completedRoutinesEmpty = stringResource(R.string.day_detail_completed_routines_empty)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.day_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
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
            item {
                Text(
                    text = date.format(formatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (items.isEmpty()) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.day_detail_empty_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(stringResource(R.string.day_detail_empty_message))
                        }
                    }
                }
            } else {
                daySection(
                    title = notesTitle,
                    emptyText = notesEmpty,
                    items = notes,
                    onItemClick = { onOpenNote(it.id) }
                )
                daySection(
                    title = remindersTitle,
                    emptyText = remindersEmpty,
                    items = reminders,
                    onItemClick = { onOpenReminder(it.id) }
                )
                daySection(
                    title = pendingRoutinesTitle,
                    emptyText = pendingRoutinesEmpty,
                    items = pendingRoutines,
                    onItemClick = { onOpenRoutineAnswer(it.id, it.date) }
                )
                daySection(
                    title = completedRoutinesTitle,
                    emptyText = completedRoutinesEmpty,
                    items = completedRoutines,
                    onItemClick = { item ->
                        item.routineExecutionId?.let { executionId ->
                            onOpenRoutineHistory(item.id, executionId)
                        }
                    }
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.daySection(
    title: String,
    emptyText: String,
    items: List<DayActivityItem>,
    onItemClick: (DayActivityItem) -> Unit
) {
    item {
        Text(title, style = MaterialTheme.typography.titleSmall)
    }

    if (items.isEmpty()) {
        item {
            Card {
                Text(
                    text = emptyText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        items(items, key = { "${it.type}-${it.id}-${it.routineExecutionId ?: 0}" }) { item ->
            DayActivityRow(item = item, onClick = { onItemClick(item) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun DayActivityRow(
    item: DayActivityItem,
    onClick: () -> Unit
) {
    val typeLabel = stringResource(item.type.labelRes())
    val subtitle = item.subtitle.ifBlank { typeLabel }

    ListItem(
        headlineContent = {
            Text(stringResource(R.string.day_detail_item_title, item.timeLabel, item.title))
        },
        supportingContent = {
            if (item.type == RecentItemType.ROUTINE) {
                Text(
                    stringResource(
                        R.string.day_detail_routine_subtitle,
                        subtitle,
                        stringResource(item.routineStatus.labelRes())
                    )
                )
            } else {
                Text(subtitle)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

private fun RecentItemType.labelRes(): Int = when (this) {
    RecentItemType.NOTE -> R.string.common_type_note
    RecentItemType.REMINDER -> R.string.common_type_reminder
    RecentItemType.ROUTINE -> R.string.common_type_routine
}

private fun RoutineExecutionStatus?.labelRes(): Int = when (this) {
    RoutineExecutionStatus.PENDING -> R.string.common_status_pending
    RoutineExecutionStatus.ANSWERED -> R.string.common_status_completed
    RoutineExecutionStatus.OMITTED -> R.string.common_status_omitted
    null -> R.string.common_status_pending
}
