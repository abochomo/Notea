package com.notea.navigation

import java.time.LocalDate

sealed class NoteaRoute(val route: String) {
    data object Home : NoteaRoute("home")
    data object NoteList : NoteaRoute("notes")
    data object NoteEditor : NoteaRoute("note-editor?noteId={noteId}")
    data object ReminderEditor : NoteaRoute("reminder-editor?reminderId={reminderId}")
    data object RoutineEditor : NoteaRoute("routine-editor?routineId={routineId}")
    data object QuestionBuilder : NoteaRoute("question-builder/{routineId}")
    data object RoutineAnswer : NoteaRoute("routine-answer/{routineId}/{date}")
    data object RoutineHistory : NoteaRoute("routine-history/{routineId}?executionId={executionId}")
    data object Calendar : NoteaRoute("calendar")
    data object DayDetail : NoteaRoute("day-detail/{date}")
    data object Settings : NoteaRoute("settings")

    companion object {
        fun noteEditor(noteId: Long? = null): String =
            if (noteId == null) "note-editor" else "note-editor?noteId=$noteId"

        fun reminderEditor(reminderId: Long? = null): String =
            if (reminderId == null) "reminder-editor" else "reminder-editor?reminderId=$reminderId"

        fun routineEditor(routineId: Long? = null): String =
            if (routineId == null) "routine-editor" else "routine-editor?routineId=$routineId"

        fun questionBuilder(routineId: Long): String = "question-builder/$routineId"

        fun routineAnswer(routineId: Long, date: LocalDate): String = "routine-answer/$routineId/$date"

        fun routineHistory(routineId: Long, executionId: Long? = null): String =
            if (executionId == null) {
                "routine-history/$routineId"
            } else {
                "routine-history/$routineId?executionId=$executionId"
            }

        fun dayDetail(date: LocalDate): String = "day-detail/$date"
    }
}
