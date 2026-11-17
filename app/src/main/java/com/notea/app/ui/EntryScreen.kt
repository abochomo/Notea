package com.notea.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.app.data.Answer
import com.notea.app.data.QType
import com.notea.app.data.Question
import com.notea.app.data.Routine
import com.notea.app.data.answersFromJson
import com.notea.app.data.answersToJson
import com.notea.app.data.questionsFromJson
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Responder una rutina para un día concreto (pasado, presente o futuro) o modificar una
 * respuesta ya guardada: si existe, se carga automáticamente.
 */
@Composable
fun EntryScreen(routineId: Long, day: Long, onBack: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme
    val colors = typeColors

    val routineState by repo.observeRoutine(routineId).collectAsStateWithLifecycle(initialValue = null as Routine?)
    val routine = routineState
    val questions = remember(routine?.questionsJson) { questionsFromJson(routine?.questionsJson ?: "[]") }

    var loaded by remember { mutableStateOf(false) }
    var exists by remember { mutableStateOf(false) }
    val answers = remember { mutableStateMapOf<String, Answer>() }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var initialKey by remember { mutableStateOf("") }
    var askDiscard by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }

    fun stateKey(): String {
        val sb = StringBuilder()
        for (key in answers.keys.sorted()) {
            val a = answers[key] ?: continue
            if (a.isEmpty) continue
            sb.append(key).append('=').append(a.text).append('|').append(a.choices.joinToString(","))
                .append('|').append(a.otherChecked).append('|').append(a.other).append('\u0001')
        }
        sb.append(content.text)
        return sb.toString()
    }

    LaunchedEffect(routineId, day) {
        val r = repo.getRoutine(routineId)
        val entry = repo.getEntry(routineId, day)
        if (entry != null) {
            exists = true
            answers.putAll(answersFromJson(entry.answersJson))
            content = TextFieldValue(entry.content)
        } else if (r != null && !r.isForm) {
            content = TextFieldValue(r.template)
        }
        initialKey = stateKey()
        loaded = true
    }

    val dirty = loaded && stateKey() != initialKey
    BackHandler(enabled = dirty) { askDiscard = true }

    val date = LocalDate.ofEpochDay(day)
    val today = LocalDate.now()

    fun save() {
        val r = routine ?: return
        if (r.isForm) {
            val missing = questions.filter { q -> q.required && (answers[q.id]?.isEmpty ?: true) }
            if (missing.isNotEmpty()) {
                val first = missing.first().title
                scope.launch {
                    snackbar.showSnackbar(
                        if (missing.size == 1) "Falta una pregunta obligatoria: $first" else "Faltan ${missing.size} preguntas obligatorias"
                    )
                }
                return
            }
            if (questions.all { q -> answers[q.id]?.isEmpty ?: true }) {
                scope.launch { snackbar.showSnackbar("Responde al menos una pregunta") }
                return
            }
        } else if (content.text.isBlank()) {
            scope.launch { snackbar.showSnackbar("La entrada está vacía") }
            return
        }
        // Se normaliza «Otro»: marcado pero sin texto equivale a no marcado.
        val clean = answers.toMap().mapValues { (_, a) ->
            if (a.otherChecked && a.other.isBlank()) a.copy(otherChecked = false, other = "") else a
        }
        scope.launch {
            repo.saveEntry(routineId, day, if (r.isForm) "" else content.text, answersToJson(clean))
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            routine?.title?.ifBlank { "Rutina" } ?: "Rutina",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(fmtDateLong(date), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (dirty) askDiscard = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (exists) {
                        IconButton(onClick = { askDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar respuesta")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = cs.surface) {
                Column(Modifier.navigationBarsPadding().imePadding()) {
                    if (routine != null && !routine.isForm) {
                        MarkdownToolbar(value = content, onValueChange = { content = it })
                    }
                    Button(
                        onClick = { save() },
                        enabled = loaded && routine != null,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(if (exists) "Guardar cambios" else "Guardar respuesta")
                    }
                }
            }
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val banner = when {
                !loaded -> null
                exists -> "Ya respondida: estás modificando la respuesta guardada."
                date.isBefore(today) -> "Día atrasado: puedes responderlo ahora."
                date.isAfter(today) -> "Día futuro: puedes dejarlo respondido por adelantado."
                else -> null
            }
            if (banner != null) {
                Surface(
                    color = if (exists) colors.routine.copy(alpha = 0.16f) else cs.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        banner,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            if (routine != null && routine.description.isNotBlank()) {
                Text(routine.description, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }

            if (routine != null && loaded) {
                if (routine.isForm) {
                    for (q in questions) {
                        val a = answers[q.id] ?: Answer()
                        QuestionCard(
                            question = q,
                            answer = a,
                            onChange = { answers[q.id] = it },
                            onPromoteOther = { text ->
                                val clean = text.trim()
                                if (clean.isNotEmpty()) {
                                    scope.launch { repo.addOption(routineId, q.id, clean) }
                                    answers[q.id] = if (q.type == QType.MULTI) {
                                        a.copy(choices = (a.choices + clean).distinct(), otherChecked = false, other = "")
                                    } else {
                                        a.copy(choices = listOf(clean), otherChecked = false, other = "")
                                    }
                                }
                            },
                        )
                    }
                    if (questions.isEmpty()) {
                        Text("Este cuestionario aún no tiene preguntas. Edita la rutina para añadirlas.", color = cs.onSurfaceVariant)
                    }
                } else {
                    MarkdownEditorField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = "Escribe la entrada de hoy…",
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 320.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (askDiscard) {
        ConfirmDialog(
            title = "¿Descartar los cambios?",
            text = "Tienes cambios sin guardar en esta respuesta.",
            confirm = "Descartar",
            onConfirm = {
                askDiscard = false
                onBack()
            },
            onDismiss = { askDiscard = false },
        )
    }
    if (askDelete) {
        ConfirmDialog(
            title = "¿Borrar la respuesta?",
            text = "Se eliminará la respuesta de este día y volverá a aparecer como pendiente.",
            confirm = "Borrar",
            onConfirm = {
                askDelete = false
                scope.launch {
                    repo.deleteEntry(routineId, day)
                    onBack()
                }
            },
            onDismiss = { askDelete = false },
        )
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    answer: Answer,
    onChange: (Answer) -> Unit,
    onPromoteOther: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                Text(
                    question.title.ifBlank { "Pregunta" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (question.required) {
                    Text(" *", style = MaterialTheme.typography.titleMedium, color = cs.error)
                }
            }
            QuestionInput(question, answer, onChange, onPromoteOther)
        }
    }
}

@Composable
private fun QuestionInput(
    q: Question,
    a: Answer,
    onChange: (Answer) -> Unit,
    onPromoteOther: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    when (q.type) {
        QType.TEXT -> OutlinedTextField(
            value = a.text,
            onValueChange = { onChange(a.copy(text = it)) },
            singleLine = true,
            placeholder = { Text("Tu respuesta") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        QType.PARAGRAPH -> OutlinedTextField(
            value = a.text,
            onValueChange = { onChange(a.copy(text = it)) },
            minLines = 3,
            placeholder = { Text("Tu respuesta") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        QType.NUMBER -> OutlinedTextField(
            value = a.text,
            onValueChange = { v -> onChange(a.copy(text = v.filter { it.isDigit() || it == '.' || it == ',' || it == '-' })) },
            singleLine = true,
            placeholder = { Text("0") },
            suffix = { if (q.unit.isNotBlank()) Text(q.unit) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        QType.SINGLE -> Column {
            for (opt in q.options) {
                val selected = !a.otherChecked && a.choices.contains(opt)
                ChoiceRow(radio = true, selected = selected, label = opt) {
                    onChange(if (selected) a.copy(choices = emptyList()) else a.copy(choices = listOf(opt), otherChecked = false))
                }
            }
            OtherRow(
                radio = true,
                checked = a.otherChecked,
                text = a.other,
                onToggle = { onChange(a.copy(otherChecked = !a.otherChecked, choices = emptyList())) },
                onText = { onChange(a.copy(other = it, otherChecked = true, choices = emptyList())) },
            )
            PromoteOther(q, a, onPromoteOther)
        }

        QType.MULTI -> Column {
            for (opt in q.options) {
                val selected = a.choices.contains(opt)
                ChoiceRow(radio = false, selected = selected, label = opt) {
                    onChange(a.copy(choices = if (selected) a.choices - opt else a.choices + opt))
                }
            }
            OtherRow(
                radio = false,
                checked = a.otherChecked,
                text = a.other,
                onToggle = { onChange(a.copy(otherChecked = !a.otherChecked)) },
                onText = { onChange(a.copy(other = it, otherChecked = true)) },
            )
            PromoteOther(q, a, onPromoteOther)
        }

        QType.DROPDOWN -> {
            var open by remember { mutableStateOf(false) }
            val current = when {
                a.otherChecked -> "Otro"
                a.choices.isNotEmpty() -> a.choices.first()
                else -> "Elige una opción"
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(current, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        for (opt in q.options) {
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    open = false
                                    onChange(a.copy(choices = listOf(opt), otherChecked = false))
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Otro…") },
                            onClick = {
                                open = false
                                onChange(a.copy(choices = emptyList(), otherChecked = true))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Sin respuesta", color = cs.onSurfaceVariant) },
                            onClick = {
                                open = false
                                onChange(a.copy(choices = emptyList(), otherChecked = false))
                            },
                        )
                    }
                }
                if (a.otherChecked) {
                    OutlinedTextField(
                        value = a.other,
                        onValueChange = { onChange(a.copy(other = it)) },
                        singleLine = true,
                        placeholder = { Text("Escribe tu respuesta") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PromoteOther(q, a, onPromoteOther)
            }
        }

        QType.SCALE -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val from = q.min.coerceIn(0, 1)
            val to = q.max.coerceIn(from + 1, 10)
            for (v in from..to) {
                val selected = a.text == v.toString()
                FilterChip(
                    selected = selected,
                    onClick = { onChange(a.copy(text = if (selected) "" else v.toString())) },
                    label = { Text(v.toString()) },
                )
            }
        }

        QType.RATING -> Row {
            val value = a.text.toIntOrNull() ?: 0
            for (v in 1..5) {
                IconButton(onClick = { onChange(a.copy(text = if (value == v) "" else v.toString())) }) {
                    Icon(
                        if (v <= value) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "$v de 5",
                        tint = if (v <= value) typeColors.reminder else cs.outline,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }

        QType.YESNO -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = a.text == "yes",
                onClick = { onChange(a.copy(text = if (a.text == "yes") "" else "yes")) },
                label = { Text("Sí") },
            )
            FilterChip(
                selected = a.text == "no",
                onClick = { onChange(a.copy(text = if (a.text == "no") "" else "no")) },
                label = { Text("No") },
            )
        }

        QType.TIME -> {
            var pick by remember { mutableStateOf(false) }
            val parts = a.text.split(':')
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pick = true }) { Text(a.text.ifBlank { "Elegir hora" }) }
                if (a.text.isNotBlank()) {
                    IconButton(onClick = { onChange(a.copy(text = "")) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Borrar hora")
                    }
                }
            }
            if (pick) {
                TimePickDialog(
                    hour = hour,
                    minute = minute,
                    onDismiss = { pick = false },
                    onPick = { h, m ->
                        pick = false
                        onChange(a.copy(text = fmtClock(h, m)))
                    },
                )
            }
        }

        QType.DATE -> {
            var pick by remember { mutableStateOf(false) }
            val current = try {
                LocalDate.parse(a.text)
            } catch (e: Exception) {
                null
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pick = true }) {
                    Text(if (current == null) "Elegir fecha" else a.display(q))
                }
                if (a.text.isNotBlank()) {
                    IconButton(onClick = { onChange(a.copy(text = "")) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Borrar fecha")
                    }
                }
            }
            if (pick) {
                DatePickDialog(
                    initial = current ?: LocalDate.now(),
                    onDismiss = { pick = false },
                    onPick = { d ->
                        pick = false
                        onChange(a.copy(text = d.toString()))
                    },
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(radio: Boolean, selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (radio) {
            RadioButton(selected = selected, onClick = null)
        } else {
            Checkbox(checked = selected, onCheckedChange = null)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Opción «Otro», presente siempre en las preguntas de opciones, con su campo de texto libre. */
@Composable
private fun OtherRow(
    radio: Boolean,
    checked: Boolean,
    text: String,
    onToggle: () -> Unit,
    onText: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle).padding(vertical = 8.dp)) {
            if (radio) {
                RadioButton(selected = checked, onClick = null)
            } else {
                Checkbox(checked = checked, onCheckedChange = null)
            }
        }
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            singleLine = true,
            placeholder = { Text("Otro…") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Botón para convertir el texto de «Otro» en una opción permanente de la pregunta. */
@Composable
private fun PromoteOther(q: Question, a: Answer, onPromote: (String) -> Unit) {
    val text = a.other.trim()
    if (a.otherChecked && text.isNotEmpty() && q.options.none { it.equals(text, ignoreCase = true) }) {
        TextButton(onClick = { onPromote(text) }) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Guardar «$text» como opción permanente", maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
