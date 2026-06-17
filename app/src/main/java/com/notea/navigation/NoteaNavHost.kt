package com.notea.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notea.R
import com.notea.presentation.calendar.CalendarRoute
import com.notea.presentation.daydetail.DayDetailRoute
import com.notea.presentation.home.HomeRoute
import com.notea.presentation.note.editor.NoteEditorRoute
import com.notea.presentation.note.list.NoteListRoute
import com.notea.presentation.reminder.editor.ReminderEditorRoute
import com.notea.presentation.routine.RoutineAnswerRoute
import com.notea.presentation.routine.RoutineEditorScreen
import com.notea.presentation.routine.RoutineHistoryRoute
import com.notea.presentation.routine.RoutineQuestionBuilderRoute
import com.notea.presentation.settings.SettingsScreen
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun NoteaNavHost(
    initialDeepLinkIntent: Intent? = null,
    newDeepLinkIntents: Flow<Intent> = emptyFlow()
) {
    val navController = rememberNavController()
    var initialDeepLinkConsumed by rememberSaveable { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = NoteaRoute.Home.route
    ) {
        composable(NoteaRoute.Home.route) {
            HomeRoute(
                onCreateNote = { navController.navigateOnce(NoteaRoute.noteEditor()) },
                onCreateReminder = { navController.navigateOnce(NoteaRoute.reminderEditor()) },
                onCreateRoutine = { navController.navigateOnce(NoteaRoute.routineEditor()) },
                onOpenNote = { navController.navigateOnce(NoteaRoute.noteEditor(it)) },
                onOpenReminder = { navController.navigateOnce(NoteaRoute.reminderEditor(it)) },
                onOpenRoutine = { navController.navigateOnce(NoteaRoute.routineEditor(it)) },
                onOpenNotes = { navController.navigateOnce(NoteaRoute.NoteList.route) },
                onOpenCalendar = { navController.navigateOnce(NoteaRoute.Calendar.route) },
                onOpenSettings = { navController.navigateOnce(NoteaRoute.Settings.route) }
            )
        }

        composable(NoteaRoute.NoteList.route) {
            NoteListRoute(
                onBack = { navController.navigateBackOrHome() },
                onCreateNote = { navController.navigateOnce(NoteaRoute.noteEditor()) },
                onOpenNote = { navController.navigateOnce(NoteaRoute.noteEditor(it)) }
            )
        }

        composable(
            route = NoteaRoute.NoteEditor.route,
            arguments = listOf(navArgument("noteId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            NoteEditorRoute(onBack = { navController.navigateBackOrHome() })
        }

        composable(
            route = NoteaRoute.ReminderEditor.route,
            arguments = listOf(navArgument("reminderId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            ReminderEditorRoute(onBack = { navController.navigateBackOrHome() })
        }

        composable(
            route = NoteaRoute.RoutineEditor.route,
            arguments = listOf(navArgument("routineId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            RoutineEditorScreen(
                onBack = { navController.navigateBackOrHome() },
                onOpenQuestionBuilder = { navController.navigateOnce(NoteaRoute.questionBuilder(it)) },
                onOpenHistory = { navController.navigateOnce(NoteaRoute.routineHistory(it)) }
            )
        }

        composable(
            route = NoteaRoute.QuestionBuilder.route,
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) {
            RoutineQuestionBuilderRoute(onBack = { navController.navigateBackOrHome() })
        }

        composable(
            route = NoteaRoute.RoutineAnswer.route,
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType },
                navArgument("date") { type = NavType.StringType }
            )
        ) {
            RoutineAnswerRoute(onBack = { navController.navigateBackOrHome() })
        }

        composable(
            route = NoteaRoute.RoutineHistory.route,
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType },
                navArgument("executionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            RoutineHistoryRoute(onBack = { navController.navigateBackOrHome() })
        }

        composable(NoteaRoute.Calendar.route) {
            CalendarRoute(
                onBack = { navController.navigateBackOrHome() },
                onOpenDay = { navController.navigateOnce(NoteaRoute.dayDetail(it)) }
            )
        }

        composable(
            route = NoteaRoute.DayDetail.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { entry ->
            val date = entry.arguments?.getString("date")
                ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
            if (date == null) {
                InvalidRouteScreen(
                    title = stringResource(R.string.nav_invalid_date_title),
                    message = stringResource(R.string.nav_invalid_date_message),
                    onBack = { navController.navigateBackOrHome() }
                )
            } else {
                DayDetailRoute(
                    date = date,
                    onBack = { navController.navigateBackOrHome() },
                    onOpenNote = { navController.navigateOnce(NoteaRoute.noteEditor(it)) },
                    onOpenReminder = { navController.navigateOnce(NoteaRoute.reminderEditor(it)) },
                    onOpenRoutineAnswer = { routineId, selectedDate ->
                        navController.navigateOnce(NoteaRoute.routineAnswer(routineId, selectedDate))
                    },
                    onOpenRoutineHistory = { routineId, executionId ->
                        navController.navigateOnce(NoteaRoute.routineHistory(routineId, executionId))
                    }
                )
            }
        }

        composable(NoteaRoute.Settings.route) {
            SettingsScreen(onBack = { navController.navigateBackOrHome() })
        }
    }

    LaunchedEffect(navController, initialDeepLinkIntent) {
        if (!initialDeepLinkConsumed && initialDeepLinkIntent.isNoteaDeepLink()) {
            initialDeepLinkConsumed = true
            navController.navigateFromNoteaDeepLink(initialDeepLinkIntent)
        }
    }

    LaunchedEffect(navController, newDeepLinkIntents) {
        newDeepLinkIntents.collect { intent ->
            if (intent.isNoteaDeepLink()) {
                navController.navigateFromNoteaDeepLink(intent)
            }
        }
    }
}

private fun NavHostController.navigateOnce(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateBackOrHome() {
    if (popBackStack()) return

    if (currentBackStackEntry?.destination?.route != NoteaRoute.Home.route) {
        navigateOnce(NoteaRoute.Home.route)
    }
}

private fun NavHostController.navigateFromNoteaDeepLink(intent: Intent?) {
    val route = intent.toNoteaRoute() ?: return
    navigate(route) {
        popUpTo(NoteaRoute.Home.route) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

private fun Intent?.isNoteaDeepLink(): Boolean =
    this?.action == Intent.ACTION_VIEW && data?.scheme == "notea"

private fun Intent?.toNoteaRoute(): String? {
    val uri = this?.data ?: return null
    if (uri.scheme != "notea") return null

    return when (uri.host) {
        "reminder" -> {
            val segments = uri.pathSegments
            if (segments.size != 1) return null

            val reminderId = segments[0].toLongOrNull()
                ?.takeIf { it > 0L }
            reminderId?.let { NoteaRoute.reminderEditor(it) }
        }
        "routine" -> {
            val segments = uri.pathSegments
            if (segments.size != 2) return null

            val routineId = segments[0].toLongOrNull()
                ?.takeIf { it > 0L }
            val date = segments[1]
                .let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

            if (routineId != null && date != null) {
                NoteaRoute.routineAnswer(routineId, date)
            } else {
                null
            }
        }
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvalidRouteScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            ) {
                Text(stringResource(R.string.common_back))
            }
        }
    }
}
