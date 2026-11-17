package com.notea.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.notea.app.data.Frequency
import com.notea.app.data.QType
import com.notea.app.data.Question
import com.notea.app.data.Routine
import com.notea.app.data.dayBit
import com.notea.app.data.dayShortName
import com.notea.app.data.formatMinutes
import com.notea.app.data.freq
import com.notea.app.data.questionsFromJson
import com.notea.app.data.questionsToJson
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** Crear o editar una rutina: tipo, frecuencia, horario y, si es cuestionario, sus preguntas. */
@Composable
fun RoutineEditorScreen(routineId: Long, day: Long, onBack: () -> Unit, onCreated: (Long) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme

    val startDate = if (day >= 0) LocalDate.ofEpochDay(day) else LocalDate.now()

    var loaded by remember { mutableStateOf(routineId == 0L) }
    var original by remember { mutableStateOf<Routine?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isForm by remember { mutableStateOf(true) }
    var template by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(Frequency.DAILY) }
    var daysMask by remember { mutableStateOf(dayBit(startDate.dayOfWeek)) }
    var dayOfMonth by remember { mutableStateOf(startDate.dayOfMonth) }
    var timeMinutes by remember { mutableStateOf(21 * 60) }
    var startDay by remember { mutableStateOf(startDate.toEpochDay()) }
    var endDay by remember { mutableStateOf<Long?>(null) }
    var notify by remember { mutableStateOf(true) }
    var active by remember { mutableStateOf(true) }
    val questions = remember { mutableStateListOf<Question>() }

    var pickTime by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }
    var initialKey by remember { mutableStateOf("") }

    fun stateKey(): String = listOf(
        title, description, isForm, template, frequency, daysMask, dayOfMonth, timeMinutes,
        startDay, endDay, notify, active, questionsToJson(questions.toList()),
    ).joinToString("\u0001")

    LaunchedEffect(routineId) {
        if (routineId != 0L) {
            val r = repo.getRoutine(routineId)
            if (r != null) {
                original = r
                title = r.title
                description = r.description
                isForm = r.isForm
                template = r.template
                frequency = r.freq
                daysMask = r.daysMask
                dayOfMonth = r.dayOfMonth
                timeMinutes = r.timeMinutes
                startDay = r.startDay
                endDay = r.endDay
                notify = r.notify
                active = r.active
                questions.clear()
                questions.addAll(questionsFromJson(r.questionsJson))
            }
            loaded = true
        }
        initialKey = stateKey()
    }

    val dirty = loaded && initialKey.isNotEmpty() && stateKey() != initialKey

    fun tryBack() {
        if (dirty) askDiscard = true else onBack()
    }
    BackHandler(enabled = dirty) { askDiscard = true }

    fun save() {
        val cleanQuestions = questions.map { q ->
            q.copy(
                title = q.title.trim(),
                options = if (q.type.hasOptions) q.options.map { it.trim() }.filter { it.isNotEmpty() }.distinct() else emptyList(),
            )
        }
        val end = endDay
        val problem = when {
            title.isBlank() -> "Ponle un título a la rutina"
            (frequency == Frequency.WEEKLY || frequency == Frequency.CUSTOM) && daysMask == 0 -> "Elige al menos un día de la semana"
            end != null && end < startDay -> "La fecha de fin es anterior a la de inicio"
            isForm && cleanQuestions.isEmpty() -> "Añade al menos una pregunta al cuestionario"
            isForm && cleanQuestions.any { it.title.isEmpty() } -> "Hay preguntas sin enunciado"
            else -> null
        }
        if (problem != null) {
            scope.launch { snackbar.showSnackbar(problem) }
            return
        }
        val now = System.currentTimeMillis()
        val base = original
        val routine = Routine(
            id = base?.id ?: 0L,
            title = title.trim(),
            description = description.trim(),
            isForm = isForm,
            template = template,
            questionsJson = questionsToJson(cleanQuestions),
            frequency = frequency.name,
            daysMask = daysMask,
            dayOfMonth = dayOfMonth,
            timeMinutes = timeMinutes,
            startDay = startDay,
            endDay = endDay,
            active = active,
            notify = notify,
            createdAt = base?.createdAt ?: now,
            updatedAt = now,
        )
        scope.launch {
            val id = repo.saveRoutine(routine)
            if (base == null) onCreated(id) else onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (routineId == 0L) "Nueva rutina" else "Editar rutina") },
                navigationIcon = {
                    IconButton(onClick = { tryBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Button(onClick = { save() }, modifier = Modifier.padding(end = 8.dp)) { Text("Guardar") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .consumeWindowInsets(inner)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (opcional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Tipo de rutina")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isForm,
                    onClick = { isForm = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Cuestionario") }
                SegmentedButton(
                    selected = !isForm,
                    onClick = { isForm = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Nota (diario)") }
            }

            SectionLabel("Frecuencia")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (f in Frequency.values()) {
                    FilterChip(
                        selected = frequency == f,
                        onClick = {
                            frequency = f
                            if (f == Frequency.WEEKLY && Integer.bitCount(daysMask) != 1) {
                                daysMask = dayBit(LocalDate.ofEpochDay(startDay).dayOfWeek)
                            }
                        },
                        label = { Text(f.label) },
                    )
                }
            }
            if (frequency == Frequency.WEEKLY || frequency == Frequency.CUSTOM) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in DayOfWeek.values()) {
                        val bit = dayBit(d)
                        FilterChip(
                            selected = (daysMask and bit) != 0,
                            onClick = {
                                daysMask = if (frequency == Frequency.WEEKLY) bit else daysMask xor bit
                            },
                            label = { Text(dayShortName(d)) },
                        )
                    }
                }
                Text(
                    if (frequency == Frequency.WEEKLY) "Se repite un día a la semana." else "Marca todos los días en los que toca la rutina.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            if (frequency == Frequency.MONTHLY) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Día del mes", modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (dayOfMonth > 1) dayOfMonth-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Día anterior")
                    }
                    Text(dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { if (dayOfMonth < 31) dayOfMonth++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Día siguiente")
                    }
                }
                if (dayOfMonth > 28) {
                    Text(
                        "En los meses más cortos se usará el último día del mes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }

            SectionLabel("Horario")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hora", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { pickTime = true }) { Text(formatMinutes(timeMinutes)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Empieza", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { pickStart = true }) { Text(fmtDate(LocalDate.ofEpochDay(startDay))) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Termina", modifier = Modifier.weight(1f))
                val end = endDay
                if (end != null) {
                    IconButton(onClick = { endDay = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar fecha de fin")
                    }
                }
                OutlinedButton(onClick = { pickEnd = true }) {
                    Text(if (end == null) "Nunca" else fmtDate(LocalDate.ofEpochDay(end)))
                }
            }
            SwitchRow(
                title = "Avisarme con una notificación",
                subtitle = "A la hora elegida, los días que toque",
                checked = notify,
                onChange = { notify = it },
            )
            if (routineId != 0L) {
                SwitchRow(
                    title = "Rutina activa",
                    subtitle = "Si la pausas dejan de generarse días pendientes; el historial se conserva",
                    checked = active,
                    onChange = { active = it },
                )
            }

            if (isForm) {
                SectionLabel("Preguntas")
                questions.forEachIndexed { index, q ->
                    QuestionEditorCard(
                        index = index,
                        question = q,
                        isFirst = index == 0,
                        isLast = index == questions.size - 1,
                        onChange = { questions[index] = it },
                        onMoveUp = {
                            if (index > 0) {
                                val item = questions.removeAt(index)
                                questions.add(index - 1, item)
                            }
                        },
                        onMoveDown = {
                            if (index < questions.size - 1) {
                                val item = questions.removeAt(index)
                                questions.add(index + 1, item)
                            }
                        },
                        onDuplicate = { questions.add(index + 1, q.copy(id = java.util.UUID.randomUUID().toString())) },
                        onDelete = { questions.removeAt(index) },
                    )
                }
                OutlinedButton(onClick = { questions.add(Question()) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Añadir pregunta")
                }
            } else {
                SectionLabel("Plantilla")
                Text(
                    "Texto con el que empezará cada entrada del diario (opcional). Admite Markdown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = template,
                    onValueChange = { template = it },
                    placeholder = { Text("## ¿Qué tal el día?\n\n- [ ] …") },
                    minLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickTime) {
        TimePickDialog(
            hour = timeMinutes / 60,
            minute = timeMinutes % 60,
            onDismiss = { pickTime = false },
            onPick = { h, m ->
                pickTime = false
                timeMinutes = h * 60 + m
            },
        )
    }
    if (pickStart) {
        DatePickDialog(
            initial = LocalDate.ofEpochDay(startDay),
            onDismiss = { pickStart = false },
            onPick = {
                pickStart = false
                startDay = it.toEpochDay()
            },
        )
    }
    if (pickEnd) {
        DatePickDialog(
            initial = LocalDate.ofEpochDay(endDay ?: startDay),
            onDismiss = { pickEnd = false },
            onPick = {
                pickEnd = false
                endDay = it.toEpochDay()
            },
        )
    }
    if (askDiscard) {
        ConfirmDialog(
            title = "¿Descartar los cambios?",
            text = "Has modificado la rutina y aún no la has guardado.",
            confirm = "Descartar",
            onConfirm = {
                askDiscard = false
                onBack()
            },
            onDismiss = { askDiscard = false },
        )
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun QuestionEditorCard(
    index: Int,
    question: Question,
    isFirst: Boolean,
    isLast: Boolean,
    onChange: (Question) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var typeMenu by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pregunta ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar")
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar pregunta")
                }
            }
            OutlinedTextField(
                value = question.title,
                onValueChange = { onChange(question.copy(title = it)) },
                label = { Text("Enunciado") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Box {
                OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(question.type.label, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    for (t in QType.values()) {
                        DropdownMenuItem(
                            text = { Text(t.label) },
                            onClick = {
                                typeMenu = false
                                var next = question.copy(type = t)
                                if (t.hasOptions && next.options.isEmpty()) next = next.copy(options = listOf("Opción 1", "Opción 2"))
                                if (t == QType.SCALE && (next.max < 2 || next.max > 10)) next = next.copy(min = 1, max = 5)
                                onChange(next)
                            },
                        )
                    }
                }
            }

            if (question.type.hasOptions) {
                question.options.forEachIndexed { i, opt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = opt,
                            onValueChange = { v ->
                                onChange(question.copy(options = question.options.toMutableList().also { it[i] = v }))
                            },
                            singleLine = true,
                            placeholder = { Text("Opción ${i + 1}") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            onChange(question.copy(options = question.options.toMutableList().also { it.removeAt(i) }))
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar opción")
                        }
                    }
                }
                TextButton(onClick = { onChange(question.copy(options = question.options + "")) }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Añadir opción")
                }
                Text(
                    "Al responder siempre aparecerá además «Otro», con un campo de texto que después podrás convertir en opción permanente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }

            if (question.type == QType.SCALE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Desde", modifier = Modifier.weight(1f))
                    FilterChip(selected = question.min == 0, onClick = { onChange(question.copy(min = 0)) }, label = { Text("0") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = question.min == 1, onClick = { onChange(question.copy(min = 1)) }, label = { Text("1") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hasta", modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (question.max > 2) onChange(question.copy(max = question.max - 1)) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Reducir máximo")
                    }
                    Text(question.max.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { if (question.max < 10) onChange(question.copy(max = question.max + 1)) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Aumentar máximo")
                    }
                }
            }

            if (question.type == QType.NUMBER) {
                OutlinedTextField(
                    value = question.unit,
                    onValueChange = { onChange(question.copy(unit = it)) },
                    label = { Text("Unidad (opcional)") },
                    placeholder = { Text("km, h, vasos…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SwitchRow(
                title = "Obligatoria",
                subtitle = null,
                checked = question.required,
                onChange = { onChange(question.copy(required = it)) },
            )
        }
    }
}
