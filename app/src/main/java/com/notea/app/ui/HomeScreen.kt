package com.notea.app.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.notea.app.data.AppSettings
import com.notea.app.data.Note
import com.notea.app.data.Routine
import com.notea.app.data.RoutineEntry
import com.notea.app.data.formatMinutes
import com.notea.app.data.frequencyText
import com.notea.app.data.occursOn
import com.notea.app.data.questionsFromJson
import com.notea.app.data.stats
import com.notea.app.data.toMillis
import com.notea.app.md.plainSnippet
import com.notea.app.md.taskProgress
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class HomeFilter(val label: String) {
    ALL("Todo"),
    NOTES("Notas"),
    REMINDERS("Recordatorios"),
    ROUTINES("Rutinas"),
    ARCHIVED("Archivadas"),
}

private sealed class HomeItem {
    abstract val key: String
    abstract val pinned: Boolean
    abstract val updated: Long
    abstract val created: Long
    abstract val sortTitle: String

    data class NoteItem(val note: Note) : HomeItem() {
        override val key: String get() = "n" + note.id
        override val pinned: Boolean get() = note.pinned
        override val updated: Long get() = note.updatedAt
        override val created: Long get() = note.createdAt
        override val sortTitle: String get() = note.title.lowercase()
    }

    data class RoutineItem(
        val routine: Routine,
        val dueToday: Boolean,
        val answeredToday: Boolean,
        val streak: Int,
    ) : HomeItem() {
        override val key: String get() = "r" + routine.id
        override val pinned: Boolean get() = false
        override val updated: Long get() = routine.updatedAt
        override val created: Long get() = routine.createdAt
        override val sortTitle: String get() = routine.title.lowercase()
    }
}

fun splitTags(tags: String): List<String> = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

@Composable
fun HomeScreen(nav: NavController, settings: AppSettings, bottomBar: @Composable () -> Unit) {
    val repo = LocalRepo.current
    val prefs = LocalPrefs.current
    val scope = rememberCoroutineScope()
    val notes by repo.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    val routines by repo.routines.collectAsStateWithLifecycle(initialValue = emptyList())
    val entries by repo.entries.collectAsStateWithLifecycle(initialValue = emptyList())

    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(HomeFilter.ALL.name) }
    val filter = HomeFilter.valueOf(filterName)
    var showCreate by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<HomeItem?>(null) }

    val today = LocalDate.now()
    val todayDay = today.toEpochDay()
    val now = System.currentTimeMillis()

    val answeredByRoutine: Map<Long, Set<Long>> = remember(entries) {
        entries.groupBy { it.routineId }.mapValues { e -> e.value.map { it.day }.toSet() }
    }

    val items: List<HomeItem> = remember(notes, routines, answeredByRoutine, query, filter, settings.sort, todayDay) {
        val q = query.trim().lowercase()
        val list = ArrayList<HomeItem>()
        if (filter != HomeFilter.ROUTINES) {
            for (n in notes) {
                val include = when (filter) {
                    HomeFilter.ARCHIVED -> n.archived
                    HomeFilter.NOTES -> !n.archived && !n.isReminder
                    HomeFilter.REMINDERS -> !n.archived && n.isReminder
                    else -> !n.archived
                }
                if (!include) continue
                if (q.isNotEmpty() && !(n.title.lowercase().contains(q) || n.content.lowercase().contains(q) || n.tags.lowercase().contains(q.removePrefix("#")))) continue
                list.add(HomeItem.NoteItem(n))
            }
        }
        if (filter == HomeFilter.ALL || filter == HomeFilter.ROUTINES) {
            for (r in routines) {
                if (q.isNotEmpty()) {
                    val inQuestions = questionsFromJson(r.questionsJson).any { it.title.lowercase().contains(q) }
                    if (!(r.title.lowercase().contains(q) || r.description.lowercase().contains(q) || inQuestions)) continue
                }
                val answered = answeredByRoutine[r.id] ?: emptySet()
                list.add(
                    HomeItem.RoutineItem(
                        routine = r,
                        dueToday = r.active && r.occursOn(today),
                        answeredToday = answered.contains(todayDay),
                        streak = r.stats(answered, today).currentStreak,
                    )
                )
            }
        }
        when (settings.sort) {
            1 -> list.sortedByDescending { it.created }
            2 -> list.sortedBy { it.sortTitle }
            else -> list.sortedByDescending { it.updated }
        }
    }
    val pinned = items.filter { it.pinned }
    val others = items.filter { !it.pinned }

    // «Para hoy»: rutinas pendientes y recordatorios de hoy o atrasados.
    val showToday = filter == HomeFilter.ALL && query.isBlank()
    val todayRoutines = remember(routines, answeredByRoutine, todayDay) {
        routines.filter { it.active && it.occursOn(today) && !(answeredByRoutine[it.id] ?: emptySet()).contains(todayDay) }
            .sortedBy { it.timeMinutes }
    }
    val todayReminders = remember(notes, todayDay) {
        val endOfToday = today.plusDays(1).atStartOfDay().toMillis()
        notes.filter { n -> n.isReminder && !n.archived && !n.reminderDone && (n.reminderAt ?: Long.MAX_VALUE) < endOfToday }
            .sortedBy { it.reminderAt ?: 0L }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Notea",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { scope.launch { prefs.setGrid(!settings.grid) } }) {
                            Icon(
                                if (settings.grid) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                                contentDescription = if (settings.grid) "Ver como lista" else "Ver en cuadrícula",
                            )
                        }
                        Box {
                            IconButton(onClick = { sortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar")
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                val labels = listOf("Última modificación", "Fecha de creación", "Título")
                                labels.forEachIndexed { i, label ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        leadingIcon = {
                                            if (settings.sort == i) Icon(Icons.Filled.Check, contentDescription = null)
                                        },
                                        onClick = {
                                            sortMenu = false
                                            scope.launch { prefs.setSort(i) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Buscar notas, recordatorios y rutinas") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(HomeFilter.values().size) { i ->
                            val f = HomeFilter.values()[i]
                            FilterChip(
                                selected = filter == f,
                                onClick = { filterName = f.name },
                                label = { Text(f.label) },
                            )
                        }
                    }
                }
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Crear") },
            )
        },
    ) { inner ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(if (settings.grid) 2 else 1),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalItemSpacing = 10.dp,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(inner).fillMaxSize(),
        ) {
            if (showToday && (todayRoutines.isNotEmpty() || todayReminders.isNotEmpty())) {
                item(key = "today", span = StaggeredGridItemSpan.FullLine) {
                    TodayCard(
                        routines = todayRoutines,
                        reminders = todayReminders,
                        now = now,
                        onRoutine = { nav.navigate(Routes.entry(it.id, todayDay)) },
                        onReminder = { nav.navigate(Routes.note(it.id)) },
                        onReminderDone = { n ->
                            scope.launch { repo.saveNote(n.copy(reminderDone = true, updatedAt = System.currentTimeMillis())) }
                        },
                    )
                }
            }
            if (items.isEmpty()) {
                item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                    if (query.isNotBlank() || filter != HomeFilter.ALL) {
                        EmptyState(Icons.Filled.Search, "Sin resultados", "Prueba con otra búsqueda o cambia el filtro.")
                    } else {
                        EmptyState(
                            Icons.Filled.Description,
                            "Todavía no hay nada",
                            "Pulsa «Crear» para añadir tu primera nota, recordatorio o rutina.",
                        )
                    }
                }
            }
            if (pinned.isNotEmpty()) {
                item(key = "h-pinned", span = StaggeredGridItemSpan.FullLine) { SectionLabel("Fijadas") }
                items(pinned, key = { it.key }) { item ->
                    HomeCard(item, nav, onDelete = { confirmDelete = item })
                }
                if (others.isNotEmpty()) {
                    item(key = "h-others", span = StaggeredGridItemSpan.FullLine) { SectionLabel("Otras") }
                }
            }
            items(others, key = { it.key }) { item ->
                HomeCard(item, nav, onDelete = { confirmDelete = item })
            }
        }
    }

    if (showCreate) {
        CreateSheet(
            subtitle = null,
            onDismiss = { showCreate = false },
            onNote = {
                showCreate = false
                nav.navigate(Routes.note())
            },
            onReminder = {
                showCreate = false
                nav.navigate(Routes.note(reminder = true))
            },
            onRoutine = {
                showCreate = false
                nav.navigate(Routes.routineEdit())
            },
        )
    }

    val toDelete = confirmDelete
    if (toDelete != null) {
        val isRoutine = toDelete is HomeItem.RoutineItem
        ConfirmDialog(
            title = if (isRoutine) "¿Eliminar la rutina?" else "¿Eliminar la nota?",
            text = if (isRoutine) "Se borrarán también todas sus respuestas. Esta acción no se puede deshacer." else "Esta acción no se puede deshacer.",
            confirm = "Eliminar",
            onConfirm = {
                confirmDelete = null
                scope.launch {
                    when (toDelete) {
                        is HomeItem.NoteItem -> repo.deleteNote(toDelete.note.id)
                        is HomeItem.RoutineItem -> repo.deleteRoutine(toDelete.routine.id)
                    }
                }
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun TodayCard(
    routines: List<Routine>,
    reminders: List<Note>,
    now: Long,
    onRoutine: (Routine) -> Unit,
    onReminder: (Note) -> Unit,
    onReminderDone: (Note) -> Unit,
) {
    val colors = typeColors
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Para hoy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            for (r in routines) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EventRepeat, contentDescription = null, tint = colors.routine, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            r.title.ifBlank { "Rutina" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Rutina · " + formatMinutes(r.timeMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                    FilledTonalButton(onClick = { onRoutine(r) }) { Text("Responder") }
                }
            }
            for (n in reminders) {
                val at = n.reminderAt ?: 0L
                val overdue = at < now
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = colors.reminder, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            n.title.ifBlank { "Recordatorio" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            (if (overdue) "Atrasado · " else "Recordatorio · ") + fmtDateTime(at),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                    IconButton(onClick = { onReminderDone(n) }) {
                        Icon(Icons.Filled.Check, contentDescription = "Marcar como hecho", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    FilledTonalButton(onClick = { onReminder(n) }) { Text("Abrir") }
                }
            }
        }
    }
}

@Composable
private fun HomeCard(item: HomeItem, nav: NavController, onDelete: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val onClick: () -> Unit = {
        when (item) {
            is HomeItem.NoteItem -> nav.navigate(Routes.note(item.note.id))
            is HomeItem.RoutineItem -> nav.navigate(Routes.routine(item.routine.id))
        }
    }
    Box {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
        ) {
            when (item) {
                is HomeItem.NoteItem -> NoteCardBody(item.note)
                is HomeItem.RoutineItem -> RoutineCardBody(item)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            when (item) {
                is HomeItem.NoteItem -> {
                    val n = item.note
                    DropdownMenuItem(
                        text = { Text(if (n.pinned) "Desfijar" else "Fijar") },
                        onClick = {
                            menu = false
                            scope.launch { repo.saveNote(n.copy(pinned = !n.pinned)) }
                        },
                    )
                    if (n.isReminder) {
                        DropdownMenuItem(
                            text = { Text(if (n.reminderDone) "Marcar como pendiente" else "Marcar como hecho") },
                            onClick = {
                                menu = false
                                scope.launch { repo.saveNote(n.copy(reminderDone = !n.reminderDone)) }
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (n.archived) "Desarchivar" else "Archivar") },
                        onClick = {
                            menu = false
                            scope.launch { repo.saveNote(n.copy(archived = !n.archived, pinned = false)) }
                        },
                    )
                }

                is HomeItem.RoutineItem -> {
                    val r = item.routine
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            menu = false
                            nav.navigate(Routes.routineEdit(r.id))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (r.active) "Pausar" else "Reanudar") },
                        onClick = {
                            menu = false
                            scope.launch { repo.saveRoutine(r.copy(active = !r.active)) }
                        },
                    )
                }
            }
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun CardHeader(color: Color, label: String, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TypeDot(color)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun NoteCardBody(note: Note) {
    val colors = typeColors
    val cs = MaterialTheme.colorScheme
    val snippet = remember(note.content) { plainSnippet(note.content, 8) }
    val progress = remember(note.content) { taskProgress(note.content) }
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CardHeader(
            color = if (note.isReminder) colors.reminder else colors.note,
            label = if (note.isReminder) "Recordatorio" else "Nota",
        ) {
            if (note.pinned) {
                Icon(Icons.Filled.PushPin, contentDescription = "Fijada", tint = cs.primary, modifier = Modifier.size(16.dp))
            }
        }
        if (note.title.isNotBlank()) {
            Text(
                note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (snippet.isNotBlank()) {
            Text(
                snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (note.title.isBlank() && snippet.isBlank()) {
            Text("Nota vacía", style = MaterialTheme.typography.bodyMedium, color = cs.outline)
        }
        if (progress.second > 0) {
            Text(
                "☑ ${progress.first} de ${progress.second}",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
        }
        val at = note.reminderAt
        if (note.isReminder && at != null) {
            val overdue = !note.reminderDone && at < System.currentTimeMillis()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (note.reminderDone) Icons.Filled.Check else Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = if (overdue) cs.error else colors.reminder,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    fmtDateTime(at),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) cs.error else cs.onSurfaceVariant,
                    textDecoration = if (note.reminderDone) TextDecoration.LineThrough else null,
                )
            }
        }
        val tags = remember(note.tags) { splitTags(note.tags) }
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (t in tags) {
                    Surface(color = cs.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "#$t",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCardBody(item: HomeItem.RoutineItem) {
    val colors = typeColors
    val cs = MaterialTheme.colorScheme
    val r = item.routine
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CardHeader(color = colors.routine, label = if (r.isForm) "Rutina · Cuestionario" else "Rutina · Diario") {
            if (item.streak > 0) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Racha", tint = colors.reminder, modifier = Modifier.size(16.dp))
                Text(item.streak.toString(), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            }
        }
        Text(
            r.title.ifBlank { "Rutina sin título" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (r.description.isNotBlank()) {
            Text(
                r.description,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(r.frequencyText(), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        val status = when {
            !r.active -> "Pausada"
            item.answeredToday -> "Respondida hoy"
            item.dueToday -> "Pendiente hoy"
            else -> null
        }
        if (status != null) {
            val bg = when {
                !r.active -> cs.surfaceContainerHighest
                item.answeredToday -> colors.routine.copy(alpha = 0.18f)
                else -> cs.tertiaryContainer
            }
            Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}
