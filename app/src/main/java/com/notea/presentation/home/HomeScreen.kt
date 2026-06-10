package com.notea.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onFilterChanged = viewModel::onFilterChanged,
        onCreateNote = onCreateNote,
        onCreateReminder = onCreateReminder,
        onCreateRoutine = onCreateRoutine,
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
    onQueryChanged: (String) -> Unit,
    onFilterChanged: (HomeFilterType) -> Unit,
    onCreateNote: () -> Unit,
    onCreateReminder: () -> Unit,
    onCreateRoutine: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecent: (RecentItem) -> Unit
) {
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notea") },
                actions = {
                    IconButton(onClick = onOpenCalendar) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Notifications, contentDescription = "Ajustes")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nuevo") }
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
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar") },
                    placeholder = { Text("Notas, recordatorios o rutinas") }
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
                            label = { Text(filter.name.lowercase().replaceFirstChar(Char::uppercase)) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Notas",
                        value = uiState.notesCount.toString(),
                        icon = { Icon(Icons.Default.Note, null) }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Recordatorios",
                        value = uiState.remindersCount.toString(),
                        icon = { Icon(Icons.Default.Notifications, null) }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Rutinas",
                        value = uiState.routinesCount.toString(),
                        icon = { Icon(Icons.Default.Repeat, null) }
                    )
                }
            }

            item {
                Text("Recientes", style = MaterialTheme.typography.titleMedium)
            }

            if (uiState.recentItems.isEmpty()) {
                item {
                    Card {
                        Text(
                            text = "No hay elementos todavía. Usa el botón Nuevo para empezar.",
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
            ListItem(
                headlineContent = { Text("Nueva nota") },
                supportingContent = { Text("Título, contenido, favorita y archivo") },
                leadingContent = { Icon(Icons.Default.Note, null) }
            )
            ListItem(
                headlineContent = { Text("Nuevo recordatorio") },
                supportingContent = { Text("Fecha, hora y notificación local") },
                leadingContent = { Icon(Icons.Default.Notifications, null) }
            )
            ListItem(
                headlineContent = { Text("Nueva rutina") },
                supportingContent = { Text("Frecuencia y horas de aviso") },
                leadingContent = { Icon(Icons.Default.Repeat, null) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { showCreateSheet = false; onCreateNote() }
                ) { Text("Nota") }

                androidx.compose.material3.FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { showCreateSheet = false; onCreateReminder() }
                ) { Text("Recordatorio") }

                androidx.compose.material3.FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { showCreateSheet = false; onCreateRoutine() }
                ) { Text("Rutina") }
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

    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = {
            Text(
                buildString {
                    append(
                        when (item.type) {
                            RecentItemType.NOTE -> "Nota"
                            RecentItemType.REMINDER -> "Recordatorio"
                            RecentItemType.ROUTINE -> "Rutina"
                        }
                    )
                    if (item.subtitle.isNotBlank()) {
                        append(" · ")
                        append(item.subtitle)
                    }
                }
            )
        },
        trailingContent = {
            Text(
                formatter.format(item.timestamp),
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
