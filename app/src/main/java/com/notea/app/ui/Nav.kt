package com.notea.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notea.app.NoteaApp
import com.notea.app.OpenTarget
import com.notea.app.data.AppSettings
import com.notea.app.notify.Notifier
import java.time.LocalDate

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"

    fun note(id: Long = 0L, reminder: Boolean = false, day: Long = -1L): String =
        "note/$id?reminder=$reminder&day=$day"

    fun routineEdit(id: Long = 0L, day: Long = -1L): String = "routineEdit/$id?day=$day"

    fun routine(id: Long): String = "routine/$id"

    fun entry(routineId: Long, day: Long): String = "entry/$routineId/$day"
}

@Composable
fun NoteaRoot(pendingTarget: MutableState<OpenTarget?>) {
    val app = LocalContext.current.applicationContext as NoteaApp
    val settingsOrNull by app.prefs.flow.collectAsStateWithLifecycle(initialValue = null as AppSettings?)
    val settings = settingsOrNull

    if (settings == null) {
        // Preferencias aún sin cargar: se evita el parpadeo de un tema equivocado.
        Box(Modifier.fillMaxSize())
        return
    }

    NoteaTheme(settings) {
        CompositionLocalProvider(LocalRepo provides app.repo, LocalPrefs provides app.prefs) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AppNav(pendingTarget, settings)
            }
        }
    }
}

@Composable
private fun AppNav(pendingTarget: MutableState<OpenTarget?>, settings: AppSettings) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val prefs = LocalPrefs.current

    // Permiso de notificaciones (Android 13+): se pide una sola vez al primer arranque.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !settings.askedNotifications && !Notifier.canNotify(ctx)) {
            prefs.setAskedNotifications(true)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Navegación al abrir desde una notificación.
    val target = pendingTarget.value
    LaunchedEffect(target) {
        if (target != null) {
            when (target) {
                is OpenTarget.OpenNote -> nav.navigate(Routes.note(target.id)) { launchSingleTop = true }
                is OpenTarget.OpenRoutine -> {
                    val day = if (target.day >= 0) target.day else LocalDate.now().toEpochDay()
                    nav.navigate(Routes.entry(target.id, day)) { launchSingleTop = true }
                }
            }
            pendingTarget.value = null
        }
    }

    val bottomBar: @Composable () -> Unit = { NoteaBottomBar(nav) }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(nav, settings, bottomBar) }
        composable(Routes.CALENDAR) { CalendarScreen(nav, bottomBar) }
        composable(Routes.SETTINGS) { SettingsScreen(settings, bottomBar) }

        composable(
            route = "note/{id}?reminder={reminder}&day={day}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("reminder") { type = NavType.BoolType; defaultValue = false },
                navArgument("day") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val args = entry.arguments
            NoteEditorScreen(
                noteId = args?.getLong("id") ?: 0L,
                startAsReminder = args?.getBoolean("reminder") ?: false,
                day = args?.getLong("day") ?: -1L,
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            route = "routineEdit/{id}?day={day}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("day") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val args = entry.arguments
            RoutineEditorScreen(
                routineId = args?.getLong("id") ?: 0L,
                day = args?.getLong("day") ?: -1L,
                onBack = { nav.popBackStack() },
                onCreated = { id ->
                    nav.popBackStack()
                    nav.navigate(Routes.routine(id))
                },
            )
        }

        composable(
            route = "routine/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            RoutineDetailScreen(
                routineId = entry.arguments?.getLong("id") ?: 0L,
                nav = nav,
            )
        }

        composable(
            route = "entry/{routineId}/{day}",
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType },
                navArgument("day") { type = NavType.LongType },
            ),
        ) { entry ->
            val args = entry.arguments
            EntryScreen(
                routineId = args?.getLong("routineId") ?: 0L,
                day = args?.getLong("day") ?: LocalDate.now().toEpochDay(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@Composable
fun NoteaBottomBar(nav: NavController) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    NavigationBar {
        BottomItem(nav, current, Routes.HOME, "Inicio") { Icon(Icons.Filled.Home, contentDescription = null) }
        BottomItem(nav, current, Routes.CALENDAR, "Calendario") { Icon(Icons.Filled.CalendarMonth, contentDescription = null) }
        BottomItem(nav, current, Routes.SETTINGS, "Ajustes") { Icon(Icons.Filled.Settings, contentDescription = null) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomItem(
    nav: NavController,
    current: String?,
    route: String,
    label: String,
    icon: @Composable () -> Unit,
) {
    NavigationBarItem(
        selected = current == route,
        onClick = {
            if (current != route) {
                nav.navigate(route) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        icon = icon,
        label = { Text(label) },
    )
}
