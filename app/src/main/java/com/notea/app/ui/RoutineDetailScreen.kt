package com.notea.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.notea.app.data.Answer
import com.notea.app.data.QType
import com.notea.app.data.Question
import com.notea.app.data.Routine
import com.notea.app.data.RoutineEntry
import com.notea.app.data.answersFromJson
import com.notea.app.data.frequencyText
import com.notea.app.data.millisToDateTime
import com.notea.app.data.nextOccurrence
import com.notea.app.data.pendingBetween
import com.notea.app.data.questionsFromJson
import com.notea.app.data.stats
import com.notea.app.md.plainSnippet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

private fun csvCell(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

/** Exporta todas las respuestas de una rutina a CSV (una fila por día). */
private fun buildCsv(routine: Routine, questions: List<Question>, entries: List<RoutineEntry>): String {
    val sb = StringBuilder()
    sb.append('﻿') // BOM para que las hojas de cálculo detecten UTF-8
    val header = ArrayList<String>()
    header.add("Fecha")
    if (routine.isForm) questions.forEach { header.add(it.title) } else header.add("Entrada")
    sb.append(header.joinToString(",") { csvCell(it) }).append("\r\n")
    for (e in entries.sortedBy { it.day }) {
        val row = ArrayList<String>()
        row.add(LocalDate.ofEpochDay(e.day).toString())
        if (routine.isForm) {
            val answers = answersFromJson(e.answersJson)
            for (q in questions) {
                val a = answers[q.id]
                row.add(
                    when {
                        a == null -> ""
                        q.type == QType.RATING || q.type == QType.SCALE || q.type == QType.NUMBER || q.type == QType.DATE -> a.text
                        else -> a.display(q)
                    }
                )
            }
        } else {
            row.add(e.content)
        }
        sb.append(row.joinToString(",") { csvCell(it) }).append("\r\n")
    }
    return sb.toString()
}

private fun fmtNumber(v: Double): String =
    if (v == Math.floor(v) && Math.abs(v) < 1e9) v.toLong().toString() else String.format(Locale.US, "%.1f", v)

/** Ficha de una rutina: estado, rachas, días pendientes, resumen de respuestas e historial. */
@Composable
fun RoutineDetailScreen(routineId: Long, nav: NavController) {
    val repo = LocalRepo.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val colors = typeColors

    val routineState by repo.observeRoutine(routineId).collectAsStateWithLifecycle(initialValue = null as Routine?)
    val entries by repo.observeEntries(routineId).collectAsStateWithLifecycle(initialValue = emptyList())
    val routine = routineState

    var menu by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var csv by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val data = csv
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        ctx.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray(Charsets.UTF_8)) }
                    } catch (e: Exception) {
                        // Si no se puede escribir, no hay nada más que hacer aquí.
                    }
                }
            }
        }
    }

    val today = LocalDate.now()
    val todayDay = today.toEpochDay()
    val questions = remember(routine?.questionsJson) { questionsFromJson(routine?.questionsJson ?: "[]") }
    val answeredDays = remember(entries) { entries.map { it.day }.toSet() }
    val parsed: List<Pair<RoutineEntry, Map<String, Answer>>> = remember(entries) {
        entries.map { Pair(it, answersFromJson(it.answersJson)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rutina", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (routine != null) {
                        IconButton(onClick = { nav.navigate(Routes.routineEdit(routine.id)) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar rutina")
                        }
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (routine.active) "Pausar" else "Reanudar") },
                                    onClick = {
                                        menu = false
                                        scope.launch { repo.saveRoutine(routine.copy(active = !routine.active)) }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Exportar respuestas (CSV)") },
                                    onClick = {
                                        menu = false
                                        csv = buildCsv(routine, questions, entries)
                                        val safe = routine.title.ifBlank { "rutina" }.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
                                        exportLauncher.launch("notea-$safe.csv")
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar", color = cs.error) },
                                    onClick = {
                                        menu = false
                                        askDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { inner ->
        if (routine == null) {
            Box(Modifier.padding(inner).fillMaxSize())
        } else {
            val st = remember(routine, answeredDays, todayDay) { routine.stats(answeredDays, today) }
            val pending = remember(routine, answeredDays, todayDay) {
                if (routine.active) {
                    routine.pendingBetween(answeredDays, today.minusDays(60), today.minusDays(1)).reversed()
                } else {
                    emptyList()
                }
            }
            val next = remember(routine, todayDay) { if (routine.active) routine.nextOccurrence(LocalDateTime.now()) else null }
            val answeredToday = answeredDays.contains(todayDay)

            LazyColumn(
                modifier = Modifier.padding(inner).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TypeBadge(Icons.Filled.EventRepeat, colors.routine)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    routine.title.ifBlank { "Rutina sin título" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    (if (routine.isForm) "Cuestionario · " else "Diario · ") + routine.frequencyText(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                        if (routine.description.isNotBlank()) {
                            Text(routine.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            when {
                                !routine.active -> "Pausada"
                                next == null -> "Sin próximas fechas"
                                else -> "Próxima: " + fmtDate(next.toLocalDate()) + " · " + fmtClock(next.hour, next.minute)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (routine.active) cs.primary else cs.outline,
                        )
                    }
                }

                item(key = "stats") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Racha", st.currentStreak.toString(), Modifier.weight(1f))
                        StatTile("Mejor racha", st.bestStreak.toString(), Modifier.weight(1f))
                        StatTile("Cumplimiento", if (st.scheduled == 0) "–" else "${Math.round(st.completion * 100)} %", Modifier.weight(1f))
                        StatTile("Respuestas", st.totalAnswers.toString(), Modifier.weight(1f))
                    }
                }

                item(key = "actions") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { nav.navigate(Routes.entry(routine.id, todayDay)) },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (answeredToday) "Editar la de hoy" else "Responder hoy") }
                        OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                            Text("Otra fecha")
                        }
                    }
                }

                if (pending.isNotEmpty()) {
                    item(key = "pending-title") { SectionLabel("Días sin responder (${pending.size})") }
                    items(pending.take(10), key = { "p" + it.toEpochDay() }) { d ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { nav.navigate(Routes.entry(routine.id, d.toEpochDay())) },
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(fmtDateLong(d), modifier = Modifier.weight(1f))
                                Text("Responder", color = cs.primary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    if (pending.size > 10) {
                        item(key = "pending-more") {
                            Text(
                                "Y ${pending.size - 10} más. Puedes responder cualquier día desde el calendario o con «Otra fecha».",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (routine.isForm && parsed.isNotEmpty() && questions.isNotEmpty()) {
                    item(key = "summary-title") { SectionLabel("Resumen de respuestas") }
                    items(questions, key = { "q" + it.id }) { q ->
                        // Respuestas de la más antigua a la más reciente.
                        val list = parsed.sortedBy { it.first.day }.mapNotNull { p -> p.second[q.id]?.takeIf { !it.isEmpty } }
                        QuestionSummary(q, list, colors.routine)
                    }
                }

                item(key = "history-title") { SectionLabel("Historial") }
                if (parsed.isEmpty()) {
                    item(key = "history-empty") {
                        Text(
                            "Todavía no hay respuestas. La primera aparecerá aquí.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
                items(parsed, key = { "e" + it.first.id }) { p ->
                    val entry = p.first
                    val preview = if (routine.isForm) {
                        questions.mapNotNull { q ->
                            val a = p.second[q.id]
                            if (a == null || a.isEmpty) null else q.title + ": " + a.display(q)
                        }.take(4).joinToString("\n")
                    } else {
                        plainSnippet(entry.content, 4)
                    }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { nav.navigate(Routes.entry(routine.id, entry.day)) },
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                fmtDateLong(LocalDate.ofEpochDay(entry.day)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (preview.isNotBlank()) {
                                Text(
                                    preview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            val edited = entry.updatedAt.millisToDateTime()
                            Text(
                                "Guardada el " + fmtDate(edited.toLocalDate()).lowercase() + " a las " + fmtClock(edited.hour, edited.minute),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.outline,
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickDate) {
        DatePickDialog(
            initial = today,
            onDismiss = { pickDate = false },
            onPick = { d ->
                pickDate = false
                nav.navigate(Routes.entry(routineId, d.toEpochDay()))
            },
        )
    }
    if (askDelete) {
        ConfirmDialog(
            title = "¿Eliminar la rutina?",
            text = "Se borrarán también todas sus respuestas. Esta acción no se puede deshacer.",
            confirm = "Eliminar",
            onConfirm = {
                askDelete = false
                scope.launch {
                    repo.deleteRoutine(routineId)
                    nav.popBackStack()
                }
            },
            onDismiss = { askDelete = false },
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainer, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Resumen agregado de una pregunta a lo largo de todas las respuestas. */
@Composable
private fun QuestionSummary(q: Question, answers: List<Answer>, accent: Color) {
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(q.title.ifBlank { "Pregunta" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (answers.isEmpty()) {
                Text("Sin respuestas todavía", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            } else {
                when (q.type) {
                    QType.NUMBER, QType.SCALE, QType.RATING -> {
                        val values = answers.mapNotNull { it.text.replace(',', '.').toDoubleOrNull() }
                        if (values.isEmpty()) {
                            Text("Sin valores numéricos", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        } else {
                            val unit = if (q.type == QType.NUMBER && q.unit.isNotBlank()) " " + q.unit else ""
                            Text(
                                "Media " + fmtNumber(values.average()) + unit + " · Mín " + fmtNumber(values.minOrNull() ?: 0.0) +
                                    " · Máx " + fmtNumber(values.maxOrNull() ?: 0.0) + " · " + values.size + " resp.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Sparkline(values.takeLast(21), accent)
                        }
                    }

                    QType.SINGLE, QType.MULTI, QType.DROPDOWN -> {
                        val counts = answers.flatMap { it.allChoices }.groupingBy { it }.eachCount()
                            .entries.sortedByDescending { it.value }
                        val max = counts.firstOrNull()?.value ?: 1
                        for (c in counts.take(8)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    c.key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                LinearProgressIndicator(
                                    progress = { c.value.toFloat() / max },
                                    color = accent,
                                    modifier = Modifier.width(90.dp).padding(horizontal = 8.dp),
                                )
                                Text(c.value.toString(), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    QType.YESNO -> {
                        val yes = answers.count { it.text == "yes" }
                        val no = answers.count { it.text == "no" }
                        val total = (yes + no).coerceAtLeast(1)
                        Text("Sí: $yes (${Math.round(yes * 100f / total)} %) · No: $no", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { yes.toFloat() / total },
                            color = accent,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    else -> {
                        Text("${answers.size} respuestas", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Última: " + answers.last().display(q),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Mini gráfico de barras con los últimos valores (el más reciente a la derecha). */
@Composable
private fun Sparkline(values: List<Double>, color: Color) {
    if (values.isEmpty()) return
    val max = values.maxOrNull() ?: 0.0
    val min = minOf(values.minOrNull() ?: 0.0, 0.0)
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(44.dp).padding(top = 4.dp)) {
        val n = values.size
        val slot = size.width / maxOf(n, 8)
        val barWidth = slot * 0.7f
        values.forEachIndexed { i, v ->
            val h = (((v - min) / range).toFloat() * size.height).coerceAtLeast(2f)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * slot + (slot - barWidth) / 2f, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
            )
        }
    }
}
