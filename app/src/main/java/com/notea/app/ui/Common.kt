package com.notea.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notea.app.data.ES
import com.notea.app.data.Prefs
import com.notea.app.data.Repository
import com.notea.app.data.millisToDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocalRepo = staticCompositionLocalOf<Repository> { error("Repository no disponible") }
val LocalPrefs = staticCompositionLocalOf<Prefs> { error("Prefs no disponible") }

// ---- Formato de fechas ----

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", ES)
private val shortDateYear: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", ES)
private val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ES)
private val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", ES)

fun String.capitalizeEs(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(ES) else it.toString() }

/** «Hoy», «Mañana», «Ayer» o una fecha corta. */
fun fmtDate(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana"
        today.minusDays(1) -> "Ayer"
        else -> (if (d.year == today.year) shortDate.format(d) else shortDateYear.format(d)).capitalizeEs()
    }
}

fun fmtDateLong(d: LocalDate): String = longDate.format(d).capitalizeEs()

fun fmtMonth(year: Int, month: Int): String = monthYear.format(LocalDate.of(year, month, 1)).capitalizeEs()

fun fmtClock(hour: Int, minute: Int): String = String.format(Locale.US, "%02d:%02d", hour, minute)

fun fmtDateTime(millis: Long): String {
    val dt = millis.millisToDateTime()
    return fmtDate(dt.toLocalDate()) + " · " + fmtClock(dt.hour, dt.minute)
}

fun fmtTimeOf(millis: Long): String {
    val dt = millis.millisToDateTime()
    return fmtClock(dt.hour, dt.minute)
}

// ---- Componentes pequeños ----

@Composable
fun TypeDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun TypeBadge(icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(56.dp),
        )
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- Diálogos ----

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } else {
                    onDismiss()
                }
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun TimePickDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val ctx = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = hour.coerceIn(0, 23),
        initialMinute = minute.coerceIn(0, 59),
        is24Hour = DateFormat.is24HourFormat(ctx),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige la hora") },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Hoja inferior para crear una nota, un recordatorio o una rutina. */
@Composable
fun CreateSheet(
    subtitle: String?,
    onDismiss: () -> Unit,
    onNote: () -> Unit,
    onReminder: () -> Unit,
    onRoutine: () -> Unit,
) {
    val colors = typeColors
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 28.dp)) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.Bottom) {
                Text("Crear", style = MaterialTheme.typography.titleLarge)
                if (subtitle != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            CreateRow(Icons.Filled.Description, colors.note, "Nota", "Texto con formato, listas y casillas", onNote)
            CreateRow(Icons.Filled.NotificationsActive, colors.reminder, "Recordatorio", "Una nota que te avisa el día y a la hora que elijas", onReminder)
            CreateRow(Icons.Filled.EventRepeat, colors.routine, "Rutina", "Un diario o cuestionario que se repite", onRoutine)
        }
    }
}

@Composable
private fun CreateRow(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = { TypeBadge(icon, color) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    )
}
