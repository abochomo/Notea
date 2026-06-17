package com.notea.domain.model

import java.time.LocalDate

data class CalendarDaySummary(
    val notes: Int = 0,
    val reminders: Int = 0,
    val routines: Int = 0
) {
    val hasActivity: Boolean
        get() = notes > 0 || reminders > 0 || routines > 0
}

data class DayActivityItem(
    val id: Long,
    val date: LocalDate,
    val timeLabel: String,
    val type: RecentItemType,
    val title: String,
    val subtitle: String,
    val routineStatus: RoutineExecutionStatus? = null,
    val routineExecutionId: Long? = null
)
