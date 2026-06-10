package com.notea.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.notea.presentation.calendar.CalendarRoute
import com.notea.presentation.daydetail.DayDetailRoute
import com.notea.presentation.home.HomeRoute
import com.notea.presentation.note.editor.NoteEditorRoute
import com.notea.presentation.reminder.editor.ReminderEditorRoute
import com.notea.presentation.routine.RoutineEditorScreen
import com.notea.presentation.routine.RoutinePlaceholderScreen
import com.notea.presentation.settings.SettingsScreen
import java.time.LocalDate

@Composable
fun NoteaNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NoteaRoute.Home.route
    ) {
        composable(NoteaRoute.Home.route) {
            HomeRoute(
                onCreateNote = { navController.navigate(NoteaRoute.noteEditor()) },
                onCreateReminder = { navController.navigate(NoteaRoute.reminderEditor()) },
                onCreateRoutine = { navController.navigate(NoteaRoute.routineEditor()) },
                onOpenNote = { navController.navigate(NoteaRoute.noteEditor(it)) },
                onOpenReminder = { navController.navigate(NoteaRoute.reminderEditor(it)) },
                onOpenRoutine = { navController.navigate(NoteaRoute.routineEditor(it)) },
                onOpenCalendar = { navController.navigate(NoteaRoute.Calendar.route) },
                onOpenSettings = { navController.navigate(NoteaRoute.Settings.route) }
            )
        }

        composable(
            route = NoteaRoute.NoteEditor.route,
            arguments = listOf(navArgument("noteId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            NoteEditorRoute(onBack = { navController.popBackStack() })
        }

        composable(
            route = NoteaRoute.ReminderEditor.route,
            arguments = listOf(navArgument("reminderId") {
                type = NavType.LongType
                defaultValue = -1L
            }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "notea://reminder/{reminderId}" }
            )
        ) {
            ReminderEditorRoute(onBack = { navController.popBackStack() })
        }

        composable(
            route = NoteaRoute.RoutineEditor.route,
            arguments = listOf(navArgument("routineId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            RoutineEditorScreen(onBack = { navController.popBackStack() })
        }

        composable(NoteaRoute.QuestionBuilder.route) {
            RoutinePlaceholderScreen(
                title = "Constructor de preguntas",
                onBack = { navController.popBackStack() }
            )
        }

        composable(NoteaRoute.RoutineAnswer.route) {
            RoutinePlaceholderScreen(
                title = "Responder rutina",
                onBack = { navController.popBackStack() }
            )
        }

        composable(NoteaRoute.RoutineHistory.route) {
            RoutinePlaceholderScreen(
                title = "Historial de rutina",
                onBack = { navController.popBackStack() }
            )
        }

        composable(NoteaRoute.Calendar.route) {
            CalendarRoute(
                onBack = { navController.popBackStack() },
                onOpenDay = { navController.navigate(NoteaRoute.dayDetail(it)) }
            )
        }

        composable(
            route = NoteaRoute.DayDetail.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { entry ->
            val date = LocalDate.parse(entry.arguments?.getString("date"))
            DayDetailRoute(
                date = date,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NoteaRoute.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
