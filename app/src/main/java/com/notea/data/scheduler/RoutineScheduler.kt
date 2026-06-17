package com.notea.data.scheduler

import java.time.LocalDate

interface RoutineScheduler {
    fun schedule(
        routineId: Long,
        scheduleMinutes: Int,
        triggerAtMillis: Long,
        scheduledDate: LocalDate
    )

    fun cancel(routineId: Long, scheduleMinutes: Int)
}
