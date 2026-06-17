package com.notea.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.domain.model.HomeFilterType
import com.notea.domain.model.RecentItem
import com.notea.domain.model.RecentItemType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(
    onCreateNote: () -> Unit,
    onCreateReminder: () -> Unit,
    onCreateRoutine: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenReminder: (Long) -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onClearSearchQuery = viewModel::clearSearchQuery,
        onFilterChanged = viewModel::onFilterChanged,
        onCreateNote = onCreateNote,
        onCreateReminder = onCreateReminder,
        onCreateRoutine = onCreateRoutine,
        onOpenNotes = onOpenNotes,
        onOpenCalendar = onOpenCalendar,
        onOpenSettings = onOpenSettings,
        onOpenRecent = {
            when (it.type) {
                RecentItemType.NOTE -> onOpenNote(it.id)
                RecentItemType.REMINDER -> onOpenReminder(it.id)
                RecentItemType.ROUTINE -> onOpenRoutine(it.id)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearchQuery: () -> Unit,
    onFilterChanged: (HomeFilterType) -> Unit,
    onCreateNote: () -> Unit,
    onCreateReminder: () -> Unit,
    onCreateRoutine: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecent: (RecentItem) -> Unit
) {
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenNotes) {
                        Icon(Icons.AutoMirrored.Filled.Note, contentDescription = stringResource(R.string.a11y_notes))
                    }
                    IconButton(onClick = onOpenCalendar) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.a11y_calendar))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.a11y_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_new)) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_search_label)) },
                    placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearchQuery) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.a11y_clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeFilterType.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.selectedFilter == filter,
                            onClick = { onFilterChanged(filter) },
                            label = { Text(stringResource(filter.labelRes())) }
                        )
                    }
                }
            }

            item {
                HomeStats(uiState = uiState)
            }

            item {
                Text(stringResource(R.string.home_recent_items), style = MaterialTheme.typography.titleMedium)
            }

            if (uiState.recentItems.isEmpty()) {
                item {
                    Card {
                        Text(
                            text = uiState.emptyMessage(searchQuery),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.recentItems, key = { "${it.type}-${it.id}" }) { item ->
                    RecentItemRow(item = item, onClick = { onOpenRecent(item) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }) {
            Text(
                text = stringResource(R.string.home_create_content),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.home_new_note)) },
                supportingContent = { Text(stringResource(R.string.home_new_note_description)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showCreateSheet = false
                        onCreateNote()
                    }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.home_new_reminder)) },
                supportingContent = { Text(stringResource(R.string.home_new_reminder_description)) },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showCreateSheet = false
                        onCreateReminder()
                    }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.home_new_routine)) },
                supportingContent = { Text(stringResource(R.string.home_new_routine_description)) },
                leadingContent = { Icon(Icons.Default.Repeat, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showCreateSheet = false
                        onCreateRoutine()
                    }
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun HomeStats(uiState: HomeUiState) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 360.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_stat_notes),
                    value = uiState.notesCount.toString(),
                    icon = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null) }
                )
                StatCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_stat_reminders),
                    value = uiState.remindersCount.toString(),
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                )
                StatCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_stat_routines),
                    value = uiState.routinesCount.toString(),
                    icon = { Icon(Icons.Default.Repeat, contentDescription = null) }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 112.dp),
                    title = stringResource(R.string.home_stat_notes),
                    value = uiState.notesCount.toString(),
                    icon = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null) }
                )
                StatCard(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 112.dp),
                    title = stringResource(R.string.home_stat_reminders),
                    value = uiState.remindersCount.toString(),
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                )
                StatCard(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 112.dp),
                    title = stringResource(R.string.home_stat_routines),
                    value = uiState.routinesCount.toString(),
                    icon = { Icon(Icons.Default.Repeat, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecentItemRow(item: RecentItem, onClick: () -> Unit) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())
    }
    val typeLabel = stringResource(item.type.labelRes())

    ListItem(
        headlineContent = { Text(item.title.ifBlank { typeLabel }) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    buildString {
                        append(typeLabel)
                        if (item.subtitle.isNotBlank()) {
                            append(" - ")
                            append(item.subtitle.trim())
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.home_updated_at, formatter.format(item.timestamp)),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        leadingContent = { Icon(item.type.icon(), contentDescription = typeLabel) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

private fun HomeFilterType.labelRes(): Int =
    when (this) {
        HomeFilterType.ALL -> R.string.home_filter_all
        HomeFilterType.NOTES -> R.string.home_filter_notes
        HomeFilterType.REMINDERS -> R.string.home_filter_reminders
        HomeFilterType.ROUTINES -> R.string.home_filter_routines
    }

private fun RecentItemType.labelRes(): Int =
    when (this) {
        RecentItemType.NOTE -> R.string.common_type_note
        RecentItemType.REMINDER -> R.string.common_type_reminder
        RecentItemType.ROUTINE -> R.string.common_type_routine
    }

private fun RecentItemType.icon(): ImageVector =
    when (this) {
        RecentItemType.NOTE -> Icons.AutoMirrored.Filled.Note
        RecentItemType.REMINDER -> Icons.Default.Notifications
        RecentItemType.ROUTINE -> Icons.Default.Repeat
    }

@Composable
private fun HomeUiState.emptyMessage(searchQuery: String): String {
    if (searchQuery.isNotBlank()) {
        return when (selectedFilter) {
            HomeFilterType.ALL -> stringResource(R.string.home_empty_search_all)
            HomeFilterType.NOTES -> stringResource(R.string.home_empty_search_notes)
            HomeFilterType.REMINDERS -> stringResource(R.string.home_empty_search_reminders)
            HomeFilterType.ROUTINES -> stringResource(R.string.home_empty_search_routines)
        }
    }

    return when (selectedFilter) {
        HomeFilterType.ALL -> stringResource(R.string.home_empty_all)
        HomeFilterType.NOTES -> stringResource(R.string.home_empty_notes)
        HomeFilterType.REMINDERS -> stringResource(R.string.home_empty_reminders)
        HomeFilterType.ROUTINES -> stringResource(R.string.home_empty_routines)
    }
}
