package com.notea.app.ui

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.notea.app.NoteaApp
import com.notea.app.data.Note
import com.notea.app.data.millisToDate
import com.notea.app.data.millisToDateTime
import com.notea.app.data.toMillis
import com.notea.app.md.toggleTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Hora por defecto de un recordatorio nuevo: la próxima hora en punto si es hoy, o las 9:00. */
private fun defaultReminderTime(date: LocalDate): Long {
    val now = LocalDateTime.now()
    return if (date == now.toLocalDate()) {
        now.plusHours(1).withMinute(0).withSecond(0).withNano(0).toMillis()
    } else {
        date.atTime(9, 0).toMillis()
    }
}

/**
 * Editor de notas y recordatorios. Guarda automáticamente mientras se escribe y al salir.
 * Las notas existentes se abren en modo lectura (Markdown renderizado); las nuevas, en edición.
 */
@Composable
fun NoteEditorScreen(noteId: Long, startAsReminder: Boolean, day: Long, onBack: () -> Unit) {
    val repo = LocalRepo.current
    val ctx = LocalContext.current
    val app = ctx.applicationContext as NoteaApp
    val scope = rememberCoroutineScope()
    val colors = typeColors
    val cs = MaterialTheme.colorScheme

    var loaded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(Note()) }
    var saved by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var editing by remember { mutableStateOf(noteId == 0L) }
    var deleted by remember { mutableStateOf(false) }
    val mutex = remember { Mutex() }

    var menu by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    var askDelete by remember { mutableStateOf(false) }
    var addTag by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId != 0L) {
            val existing = repo.getNote(noteId)
            if (existing != null) {
                note = existing
                saved = existing
                title = existing.title
                body = TextFieldValue(existing.content)
            }
        } else {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            val base = if (day >= 0) LocalDate.ofEpochDay(day) else today
            // Una nota creada desde el calendario pertenece al día elegido.
            val created = if (base == today) now else base.atTime(LocalTime.now()).toMillis()
            note = if (startAsReminder) {
                val at = defaultReminderTime(base)
                Note(
                    isReminder = true,
                    reminderAt = at,
                    notifiedAt = if (at <= now) at else null,
                    createdAt = now,
                    updatedAt = now,
                )
            } else {
                Note(createdAt = created, updatedAt = now)
            }
        }
        loaded = true
    }

    suspend fun persist() {
        mutex.withLock {
            if (!loaded || deleted) return@withLock
            val base = note.copy(title = title.trim(), content = body.text)
            val prev = saved
            if (prev != null && base.copy(updatedAt = prev.updatedAt) == prev) return@withLock
            if (base.id == 0L && base.title.isBlank() && base.content.isBlank()) return@withLock
            val toStore = base.copy(updatedAt = System.currentTimeMillis())
            val newId = repo.saveNote(toStore)
            val stored = toStore.copy(id = newId)
            saved = stored
            note = note.copy(id = newId, updatedAt = stored.updatedAt)
        }
    }

    // Guardado automático con un pequeño retardo tras cada cambio.
    LaunchedEffect(title, body.text, note, loaded) {
        if (loaded) {
            delay(500)
            persist()
        }
    }

    // Guardado al salir de la pantalla o al enviar la app a segundo plano.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) app.scope.launch { persist() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            app.scope.launch { persist() }
        }
    }

    fun setReminder(at: Long) {
        val now = System.currentTimeMillis()
        note = note.copy(
            isReminder = true,
            reminderAt = at,
            reminderDone = false,
            notifiedAt = if (at <= now) at else null,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (note.isReminder) "Recordatorio" else "Nota",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurfaceVariant,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { note = note.copy(pinned = !note.pinned) }) {
                        Icon(
                            if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.pinned) "Desfijar" else "Fijar",
                            tint = if (note.pinned) cs.primary else cs.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { editing = !editing }) {
                        Icon(
                            if (editing) Icons.Filled.Visibility else Icons.Filled.Edit,
                            contentDescription = if (editing) "Vista previa" else "Editar",
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (note.isReminder) {
                                DropdownMenuItem(
                                    text = { Text("Quitar recordatorio") },
                                    onClick = {
                                        menu = false
                                        note = note.copy(isReminder = false, reminderAt = null, reminderDone = false, notifiedAt = null)
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Añadir recordatorio") },
                                    onClick = {
                                        menu = false
                                        setReminder(defaultReminderTime(LocalDate.now()))
                                        pickDate = true
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Añadir etiqueta") },
                                onClick = {
                                    menu = false
                                    addTag = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.archived) "Desarchivar" else "Archivar") },
                                onClick = {
                                    menu = false
                                    note = note.copy(archived = !note.archived, pinned = false)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Compartir") },
                                onClick = {
                                    menu = false
                                    val text = (if (title.isNotBlank()) title.trim() + "\n\n" else "") + body.text
                                    val send = Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_SUBJECT, title.trim())
                                        .putExtra(Intent.EXTRA_TEXT, text)
                                    ctx.startActivity(Intent.createChooser(send, "Compartir nota"))
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
                },
            )
        },
        bottomBar = {
            if (editing) {
                MarkdownToolbar(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                )
            }
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it.replace("\n", " ") },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = cs.onSurface, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(cs.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "Título",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        innerField()
                    }
                },
            )

            if (loaded) {
                val created = note.createdAt.millisToDate()
                Text(
                    "Creada: " + fmtDate(created) + (if (note.id != 0L) " · Editada: " + fmtDateTime(note.updatedAt) else ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.outline,
                )
            }

            val reminderAt = note.reminderAt
            if (note.isReminder && reminderAt != null) {
                val overdue = !note.reminderDone && reminderAt < System.currentTimeMillis()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = { pickDate = true },
                        label = { Text(fmtDateTime(reminderAt)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = if (overdue) cs.error else colors.reminder,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                    FilterChip(
                        selected = note.reminderDone,
                        onClick = { note = note.copy(reminderDone = !note.reminderDone) },
                        label = { Text(if (note.reminderDone) "Hecho" else "Marcar hecho") },
                        leadingIcon = {
                            if (note.reminderDone) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                    )
                }
                if (overdue && note.notifiedAt != null) {
                    Text("Este recordatorio ya ha pasado.", style = MaterialTheme.typography.labelMedium, color = cs.error)
                }
            }

            val tags = splitTags(note.tags)
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (t in tags) {
                        InputChip(
                            selected = false,
                            onClick = { note = note.copy(tags = tags.filter { it != t }.joinToString(",")) },
                            label = { Text("#$t") },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Quitar etiqueta", modifier = Modifier.size(16.dp)) },
                        )
                    }
                    AssistChip(
                        onClick = { addTag = true },
                        label = { Text("Etiqueta") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
            }

            if (editing) {
                MarkdownEditorField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = "Escribe tu nota… Usa la barra inferior para dar formato.",
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 320.dp),
                )
            } else if (body.text.isBlank()) {
                Text(
                    "Nota vacía. Pulsa el lápiz para empezar a escribir.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.outline,
                )
            } else {
                MarkdownView(
                    text = body.text,
                    modifier = Modifier.fillMaxWidth(),
                    onToggleTask = { line -> body = TextFieldValue(toggleTask(body.text, line)) },
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    // ---- Diálogos ----

    if (pickDate) {
        val initial = (note.reminderAt ?: System.currentTimeMillis()).millisToDate()
        DatePickDialog(
            initial = initial,
            onDismiss = { pickDate = false },
            onPick = { d ->
                pickDate = false
                pendingDate = d
                pickTime = true
            },
        )
    }
    if (pickTime) {
        val current = (note.reminderAt ?: System.currentTimeMillis()).millisToDateTime()
        TimePickDialog(
            hour = current.hour,
            minute = current.minute,
            onDismiss = { pickTime = false },
            onPick = { h, m ->
                pickTime = false
                val d = pendingDate ?: current.toLocalDate()
                setReminder(d.atTime(h, m).toMillis())
            },
        )
    }
    if (askDelete) {
        ConfirmDialog(
            title = "¿Eliminar la nota?",
            text = "Esta acción no se puede deshacer.",
            confirm = "Eliminar",
            onConfirm = {
                askDelete = false
                deleted = true
                val id = note.id
                scope.launch {
                    mutex.withLock { if (id != 0L) repo.deleteNote(id) }
                    onBack()
                }
            },
            onDismiss = { askDelete = false },
        )
    }
    if (addTag) {
        var tag by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addTag = false },
            title = { Text("Nueva etiqueta") },
            text = {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it.replace(",", " ").replace("#", "") },
                    singleLine = true,
                    placeholder = { Text("p. ej. compra, trabajo…") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = tag.trim()
                    if (clean.isNotEmpty()) {
                        val current = splitTags(note.tags)
                        if (current.none { it.equals(clean, ignoreCase = true) }) {
                            note = note.copy(tags = (current + clean).joinToString(","))
                        }
                    }
                    addTag = false
                }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { addTag = false }) { Text("Cancelar") } },
        )
    }
}
