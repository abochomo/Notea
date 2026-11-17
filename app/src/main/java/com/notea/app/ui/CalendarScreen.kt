package com.notea.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.notea.app.data.Note
import com.notea.app.data.Routine
import com.notea.app.data.dayShortName
import com.notea.app.data.formatMinutes
import com.notea.app.data.millisToDate
import com.notea.app.data.occursOn
import com.notea.app.md.plainSnippet
import java.time.DayOfWeek
import java.time.LocalDate

/** Marcas de un día en el calendario. */
private class DayMarks(
    val notes: Int,
    val reminders: Int,
    val routinesAnswered: Int,
    val routinesPending: Int,
)

private class RoutineOnDay(val routine: Routine, val answered: Boolean)

@Composable
fun CalendarScreen(nav: NavController, bottomBar: @Composable () -> Unit) {
    val repo = LocalRepo.current
    val colors = typeColors
    val cs = MaterialTheme.colorScheme
    val notes by repo.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    val routines by repo.routines.collectAsStateWithLifecycle(initialValue = emptyList())
    val entries by repo.entries.collectAsStateWithLifecycle(initialValue = emptyList())

    val today = LocalDate.now()
    var monthIndex by rememberSaveable { mutableIntStateOf(today.year * 12 + (today.monthValue - 1)) }
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var showCreate by remember { mutableStateOf(false) }
    val selected = LocalDate.ofEpochDay(selectedDay)

    // Índices por día (epochDay) para pintar las marcas y la agenda.
    val activeNotes = remember(notes) { notes.filter { !it.archived } }
    val notesByDay: Map<Long, List<Note>> = remember(activeNotes) {
        activeNotes.filter { !it.isReminder }.groupBy { it.createdAt.millisToDate().toEpochDay() }
    }
    val remindersByDay: Map<Long, List<Note>> = remember(activeNotes) {
        activeNotes.filter { it.isReminder && it.reminderAt != null }
            .groupBy { (it.reminderAt ?: 0L).millisToDate().toEpochDay() }
    }
    val answeredSet: Set<Pair<Long, Long>> = remember(entries) { entries.map { Pair(it.routineId, it.day) }.toSet() }
    val routinesById = remember(routines) { routines.associateBy { it.id } }
    val entriesByDay: Map<Long, List<Long>> = remember(entries) {
        entries.groupBy({ it.day }, { it.routineId })
    }

    fun routinesOn(date: LocalDate): List<RoutineOnDay> {
        val day = date.toEpochDay()
        val out = ArrayList<RoutineOnDay>()
        val answeredIds = entriesByDay[day] ?: emptyList()
        for (id in answeredIds) {
            val r = routinesById[id]
            if (r != null) out.add(RoutineOnDay(r, true))
        }
        for (r in routines) {
            if (r.active && r.occursOn(date) && !answeredSet.contains(Pair(r.id, day))) out.add(RoutineOnDay(r, false))
        }
        return out.sortedBy { it.routine.timeMinutes }
    }

    fun marksFor(date: LocalDate): DayMarks {
        val day = date.toEpochDay()
        val rs = routinesOn(date)
        return DayMarks(
            notes = notesByDay[day]?.size ?: 0,
            reminders = remindersByDay[day]?.size ?: 0,
            routinesAnswered = rs.count { it.answered },
            routinesPending = rs.count { !it.answered },
        )
    }

    fun changeMonth(delta: Int) {
        monthIndex += delta
        val y = monthIndex / 12
        val m = monthIndex % 12 + 1
        // Al cambiar de mes se selecciona hoy (si cae dentro) o el día 1.
        selectedDay = if (today.year == y && today.monthValue == m) today.toEpochDay() else LocalDate.of(y, m, 1).toEpochDay()
    }

    val year = monthIndex / 12
    val month = monthIndex % 12 + 1

    val dayNotes = notesByDay[selectedDay] ?: emptyList()
    val dayReminders = (remindersByDay[selectedDay] ?: emptyList()).sortedBy { it.reminderAt ?: 0L }
    val dayRoutines = remember(selectedDay, routines, entries) { routinesOn(selected) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fmtMonth(year, month), fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = {
                        monthIndex = today.year * 12 + (today.monthValue - 1)
                        selectedDay = today.toEpochDay()
                    }) { Text("Hoy") }
                    IconButton(onClick = { changeMonth(-1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    IconButton(onClick = { changeMonth(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Mes siguiente")
                    }
                },
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear en este día")
            }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "grid") {
                Column(
                    Modifier.pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total > 90f) changeMonth(-1) else if (total < -90f) changeMonth(1)
                            },
                        ) { _, dragAmount -> total += dragAmount }
                    }
                ) {
                    WeekHeader()
                    AnimatedContent(
                        targetState = monthIndex,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
                            } else {
                                (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { it / 2 } + fadeOut())
                            }
                        },
                        label = "mes",
                    ) { index ->
                        MonthGrid(
                            year = index / 12,
                            month = index % 12 + 1,
                            today = today,
                            selectedDay = selectedDay,
                            marksFor = { marksFor(it) },
                            onSelect = { selectedDay = it.toEpochDay() },
                        )
                    }
                    Legend()
                }
            }

            item(key = "day-title") {
                Text(
                    fmtDateLong(selected),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (dayNotes.isEmpty() && dayReminders.isEmpty() && dayRoutines.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "No hay nada este día. Pulsa + para añadir una nota, un recordatorio o una rutina.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }

            items(dayRoutines, key = { "r" + it.routine.id }) { item ->
                val r = item.routine
                val status = when {
                    item.answered -> "Respondida"
                    selected.isBefore(today) -> "Sin responder"
                    selected == today -> "Pendiente"
                    else -> "Programada"
                }
                AgendaRow(
                    icon = Icons.Filled.EventRepeat,
                    color = colors.routine,
                    title = r.title.ifBlank { "Rutina" },
                    subtitle = "Rutina · " + formatMinutes(r.timeMinutes),
                    status = status,
                    statusFilled = item.answered,
                    statusWarning = !item.answered && selected.isBefore(today),
                    action = if (item.answered) "Editar respuesta" else "Responder",
                    onAction = { nav.navigate(Routes.entry(r.id, selectedDay)) },
                    onClick = { nav.navigate(Routes.entry(r.id, selectedDay)) },
                )
            }
            items(dayReminders, key = { "m" + it.id }) { n ->
                val at = n.reminderAt ?: 0L
                val past = at < System.currentTimeMillis()
                AgendaRow(
                    icon = Icons.Filled.NotificationsActive,
                    color = colors.reminder,
                    title = n.title.ifBlank { "Recordatorio" },
                    subtitle = "Recordatorio · " + fmtTimeOf(at),
                    status = when {
                        n.reminderDone -> "Hecho"
                        past -> "Pasado"
                        else -> "Pendiente"
                    },
                    statusFilled = n.reminderDone,
                    statusWarning = false,
                    strike = n.reminderDone,
                    action = null,
                    onAction = {},
                    onClick = { nav.navigate(Routes.note(n.id)) },
                )
            }
            items(dayNotes, key = { "n" + it.id }) { n ->
                val snippet = plainSnippet(n.content, 1)
                AgendaRow(
                    icon = Icons.Filled.Description,
                    color = colors.note,
                    title = n.title.ifBlank { snippet.ifBlank { "Nota vacía" } },
                    subtitle = if (n.title.isNotBlank() && snippet.isNotBlank()) snippet else "Nota",
                    status = null,
                    statusFilled = false,
                    statusWarning = false,
                    action = null,
                    onAction = {},
                    onClick = { nav.navigate(Routes.note(n.id)) },
                )
            }
        }
    }

    if (showCreate) {
        CreateSheet(
            subtitle = "el " + fmtDate(selected).lowercase(),
            onDismiss = { showCreate = false },
            onNote = {
                showCreate = false
                nav.navigate(Routes.note(day = selectedDay))
            },
            onReminder = {
                showCreate = false
                nav.navigate(Routes.note(reminder = true, day = selectedDay))
            },
            onRoutine = {
                showCreate = false
                nav.navigate(Routes.routineEdit(day = selectedDay))
            },
        )
    }
}

@Composable
private fun WeekHeader() {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        for (d in DayOfWeek.values()) {
            Text(
                dayShortName(d),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    today: LocalDate,
    selectedDay: Long,
    marksFor: (LocalDate) -> DayMarks,
    onSelect: (LocalDate) -> Unit,
) {
    val first = LocalDate.of(year, month, 1)
    val offset = first.dayOfWeek.value - 1 // la semana empieza en lunes
    val days = first.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    Column {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val n = row * 7 + col - offset + 1
                    if (n < 1 || n > days) {
                        Spacer(Modifier.weight(1f).height(54.dp))
                    } else {
                        val date = first.withDayOfMonth(n)
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date.toEpochDay() == selectedDay,
                            marks = marksFor(date),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    marks: DayMarks,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val colors = typeColors
    Column(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val numberMod = when {
            isSelected -> Modifier.size(30.dp).clip(CircleShape).background(cs.primary)
            isToday -> Modifier.size(30.dp).clip(CircleShape).border(1.5.dp, cs.primary, CircleShape)
            else -> Modifier.size(30.dp)
        }
        Box(numberMod, contentAlignment = Alignment.Center) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> cs.onPrimary
                    isToday -> cs.primary
                    else -> cs.onSurface
                },
            )
        }
        Row(
            Modifier.height(10.dp).padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (marks.notes > 0) Mark(colors.note, filled = true)
            if (marks.reminders > 0) Mark(colors.reminder, filled = true)
            if (marks.routinesAnswered > 0) Mark(colors.routine, filled = true)
            if (marks.routinesPending > 0) Mark(colors.routine, filled = false)
        }
    }
}

/** Punto relleno = hay elemento / rutina respondida; anillo = rutina asignada sin responder. */
@Composable
private fun Mark(color: Color, filled: Boolean) {
    if (filled) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
    } else {
        Box(Modifier.size(7.dp).border(1.5.dp, color, CircleShape))
    }
}

@Composable
private fun Legend() {
    val colors = typeColors
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LegendItem(colors.note, true, "Nota")
        LegendItem(colors.reminder, true, "Recordatorio")
        LegendItem(colors.routine, true, "Rutina hecha")
        LegendItem(colors.routine, false, "Rutina pendiente")
    }
}

@Composable
private fun LegendItem(color: Color, filled: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Mark(color, filled)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AgendaRow(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    status: String?,
    statusFilled: Boolean,
    statusWarning: Boolean,
    action: String?,
    onAction: () -> Unit,
    onClick: () -> Unit,
    strike: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(icon, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (status != null) {
                    val bg = when {
                        statusFilled -> color.copy(alpha = 0.20f)
                        statusWarning -> cs.errorContainer
                        else -> Color.Transparent
                    }
                    val border = if (!statusFilled && !statusWarning) color else Color.Transparent
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, border, RoundedCornerShape(8.dp)),
                    ) {
                        Text(
                            status,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (statusWarning) cs.onErrorContainer else cs.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                if (action != null) {
                    TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text(action)
                    }
                }
            }
        }
    }
}
