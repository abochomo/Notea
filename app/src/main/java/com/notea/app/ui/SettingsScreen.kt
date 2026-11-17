package com.notea.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.app.data.AppSettings
import com.notea.app.notify.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(settings: AppSettings, bottomBar: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val prefs = LocalPrefs.current
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme

    val notes by repo.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    val routines by repo.routines.collectAsStateWithLifecycle(initialValue = emptyList())
    val entries by repo.entries.collectAsStateWithLifecycle(initialValue = emptyList())

    // El estado de los permisos puede cambiar mientras se está en los ajustes del sistema:
    // se vuelve a comprobar cada vez que la pantalla se reanuda.
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val canNotify = remember(resumeTick) { Notifier.canNotify(ctx) }
    val canExact = remember(resumeTick) { Notifier.canScheduleExact(ctx) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        resumeTick++
    }

    var pendingBackup by remember { mutableStateOf("") }
    var askImport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val data = pendingBackup
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        ctx.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray(Charsets.UTF_8)) }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                snackbar.showSnackbar(if (ok) "Copia de seguridad guardada" else "No se pudo guardar la copia")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val message = try {
                    val json = withContext(Dispatchers.IO) {
                        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                    }
                    val count = repo.importJson(json)
                    "Copia restaurada ($count elementos)"
                } catch (e: Exception) {
                    "El archivo no es una copia válida de Notea"
                }
                snackbar.showSnackbar(message)
            }
        }
    }

    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustes", fontWeight = FontWeight.SemiBold) }) },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- Apariencia ----
            SettingsCard("Apariencia") {
                Text("Tema", style = MaterialTheme.typography.bodyLarge)
                val themes = listOf("Sistema", "Claro", "Oscuro")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    themes.forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = settings.themeMode == i,
                            onClick = { scope.launch { prefs.setThemeMode(i) } },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = themes.size),
                        ) { Text(label) }
                    }
                }

                Text("Paleta de colores", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (p in Palettes) {
                        if (p.key == "dynamic" && !dynamicColorAvailable) continue
                        val selected = settings.palette == p.key
                        val preview = if (p.key == "dynamic") cs.primary else schemeFromSeed(p.seed, LocalDark.current).primary
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { scope.launch { prefs.setPalette(p.key) } }
                                .padding(4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (p.key == "dynamic" && !selected) p.seed else preview)
                                    .border(if (selected) 3.dp else 0.dp, if (selected) cs.onSurface else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                p.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) cs.onSurface else cs.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (dynamicColorAvailable) {
                    Text(
                        "«Dinámico» toma los colores de tu fondo de pantalla (Material You).",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                SwitchRow(
                    title = "Negro puro en modo oscuro",
                    subtitle = "Ideal para pantallas AMOLED",
                    checked = settings.amoled,
                    onChange = { v -> scope.launch { prefs.setAmoled(v) } },
                )
            }

            // ---- Notificaciones ----
            SettingsCard("Notificaciones") {
                StatusRow(
                    title = "Permiso de notificaciones",
                    ok = canNotify,
                    okText = "Concedido",
                    badText = "Sin permiso: no recibirás recordatorios ni avisos de rutinas",
                )
                if (!canNotify) {
                    FilledTonalButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        // Si el sistema ya no muestra el diálogo, se abre la pantalla de ajustes de la app.
                        if (Build.VERSION.SDK_INT < 33 || settings.askedNotifications) {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                ctx.startActivity(intent)
                            } catch (e: Exception) {
                                // Sin pantalla de ajustes disponible.
                            }
                        }
                    }) { Text("Permitir notificaciones") }
                }
                StatusRow(
                    title = "Alarmas exactas",
                    ok = canExact,
                    okText = "Los avisos llegan a la hora exacta",
                    badText = "Sin permiso: los avisos pueden retrasarse unos minutos",
                )
                if (!canExact && Build.VERSION.SDK_INT >= 31) {
                    FilledTonalButton(onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:" + ctx.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            ctx.startActivity(intent)
                        } catch (e: Exception) {
                            // Sin pantalla de ajustes disponible.
                        }
                    }) { Text("Permitir alarmas exactas") }
                }
                OutlinedButton(onClick = {
                    Notifier.showTest(ctx)
                    if (!canNotify) scope.launch { snackbar.showSnackbar("Primero concede el permiso de notificaciones") }
                }) { Text("Enviar notificación de prueba") }
                Text(
                    "Si tu móvil cierra apps en segundo plano de forma agresiva, excluye Notea del ahorro de batería para no perder avisos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }

            // ---- Datos ----
            SettingsCard("Tus datos") {
                val reminders = notes.count { it.isReminder }
                Text(
                    "${notes.size - reminders} notas · $reminders recordatorios · ${routines.size} rutinas · ${entries.size} respuestas",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Todo se guarda solo en este dispositivo. Exporta una copia de vez en cuando para no perderlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                pendingBackup = repo.exportJson()
                                exportLauncher.launch("notea-copia-" + LocalDate.now().toString() + ".json")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Exportar copia") }
                    OutlinedButton(onClick = { askImport = true }, modifier = Modifier.weight(1f)) { Text("Importar copia") }
                }
            }

            // ---- Acerca de ----
            SettingsCard("Acerca de") {
                Text("Notea $version", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Notas, recordatorios y rutinas en un mismo sitio, con calendario y formato Markdown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }

    if (askImport) {
        ConfirmDialog(
            title = "¿Importar una copia?",
            text = "Los datos actuales se sustituirán por los de la copia. Si quieres conservarlos, exporta antes una copia.",
            confirm = "Elegir archivo",
            onConfirm = {
                askImport = false
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            },
            onDismiss = { askImport = false },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun StatusRow(title: String, ok: Boolean, okText: String, badText: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (ok) Color(0xFF2E9E5B) else cs.error)
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (ok) okText else badText,
                style = MaterialTheme.typography.bodySmall,
                color = if (ok) cs.onSurfaceVariant else cs.error,
            )
        }
    }
}
