package com.notea.data.repository

import android.content.Context
import com.notea.R
import com.notea.data.local.dao.NoteDao
import com.notea.data.local.dao.ReminderDao
import com.notea.data.local.dao.RoutineDao
import com.notea.data.local.entity.NoteEntity
import com.notea.data.local.entity.ReminderEntity
import com.notea.data.local.entity.RoutineEntity
import com.notea.data.local.entity.RoutineExecutionEntity
import com.notea.data.local.entity.RoutineScheduleEntity
import com.notea.domain.model.CalendarDaySummary
import com.notea.domain.model.DayActivityItem
import com.notea.domain.model.RecentItemType
import com.notea.domain.model.RoutineExecutionStatus
import com.notea.domain.repository.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao,
    private val routineDao: RoutineDao,
    private val clock: Clock
) : CalendarRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMonth(month: YearMonth): Flow<Map<LocalDate, CalendarDaySummary>> {
        val zoneId = clock.zone
        val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        return routineDao.observeActiveRoutinesCreatedBefore(end)
            .flatMapLatest { routines ->
                val routineIds = routines.map { it.routineId }
                val schedulesFlow = if (routineIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    routineDao.observeSchedulesForRoutines(routineIds)
                }

                combine(
                    noteDao.observeActivityBetween(start, end),
                    reminderDao.observeActivityBetween(start, end),
                    schedulesFlow,
                    routineDao.observeExecutionsBetween(start, end)
                ) { notes, reminders, schedules, executions ->
                    buildMonthSummary(
                        month = month,
                        zoneId = zoneId,
                        notes = notes,
                        reminders = reminders,
                        routines = routines,
                        executions = executions
                    )
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDay(date: LocalDate): Flow<List<DayActivityItem>> {
        val zoneId = clock.zone
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        return routineDao.observeActiveRoutinesCreatedBefore(end)
            .flatMapLatest { routines ->
                val routineIds = routines.map { it.routineId }
                val schedulesFlow = if (routineIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    routineDao.observeSchedulesForRoutines(routineIds)
                }

                combine(
                    noteDao.observeActivityBetween(start, end),
                    reminderDao.observeActivityBetween(start, end),
                    schedulesFlow,
                    routineDao.observeExecutionsBetween(start, end)
                ) { notes, reminders, schedules, executions ->
                    buildDayItems(
                        date = date,
                        zoneId = zoneId,
                        notes = notes,
                        reminders = reminders,
                        routines = routines,
                        schedules = schedules,
                        executions = executions,
                        noteUntitled = context.getString(R.string.note_untitled)
                    )
                }
            }
    }
}

private fun buildMonthSummary(
    month: YearMonth,
    zoneId: ZoneId,
    notes: List<NoteEntity>,
    reminders: List<ReminderEntity>,
    routines: List<RoutineEntity>,
    executions: List<RoutineExecutionEntity>
): Map<LocalDate, CalendarDaySummary> {
        val map = mutableMapOf<LocalDate, CalendarDaySummary>()

        notes.forEach { note ->
            val createdDay = Instant.ofEpochMilli(note.createdAt).atZone(zoneId).toLocalDate()
            map[createdDay] = (map[createdDay] ?: CalendarDaySummary()).copy(
                notes = (map[createdDay]?.notes ?: 0) + 1
            )
            val updatedDay = Instant.ofEpochMilli(note.updatedAt).atZone(zoneId).toLocalDate()
            if (updatedDay != createdDay) {
                map[updatedDay] = (map[updatedDay] ?: CalendarDaySummary()).copy(
                    notes = (map[updatedDay]?.notes ?: 0) + 1
                )
            }
        }

        reminders.forEach { reminder ->
            val day = Instant.ofEpochMilli(reminder.remindAt).atZone(zoneId).toLocalDate()
            map[day] = (map[day] ?: CalendarDaySummary()).copy(
                reminders = (map[day]?.reminders ?: 0) + 1
            )
        }

        val routineOccurrences = mutableSetOf<Pair<Long, LocalDate>>()
        var day = month.atDay(1)
        val lastDay = month.atEndOfMonth()
        while (!day.isAfter(lastDay)) {
            routines
                .filter { it.isDueOn(day, zoneId) }
                .forEach { routine -> routineOccurrences += routine.routineId to day }
            day = day.plusDays(1)
        }
        executions.forEach { execution ->
            val executionDay = Instant.ofEpochMilli(execution.scheduledDate).atZone(zoneId).toLocalDate()
            routineOccurrences += execution.routineId to executionDay
        }
        routineOccurrences
            .groupingBy { it.second }
            .eachCount()
            .forEach { (routineDay, count) ->
                map[routineDay] = (map[routineDay] ?: CalendarDaySummary()).copy(
                    routines = count
                )
            }

        return map
    }

private fun buildDayItems(
    date: LocalDate,
    zoneId: ZoneId,
    notes: List<NoteEntity>,
    reminders: List<ReminderEntity>,
    routines: List<RoutineEntity>,
    schedules: List<RoutineScheduleEntity>,
    executions: List<RoutineExecutionEntity>,
    noteUntitled: String
): List<DayActivityItem> {
        val items = mutableListOf<DayActivityItem>()

        notes.filter { note ->
            Instant.ofEpochMilli(note.createdAt).atZone(zoneId).toLocalDate() == date
        }.forEach { note ->
            val createdAt = Instant.ofEpochMilli(note.createdAt).atZone(zoneId).toLocalDateTime()
            items += DayActivityItem(
                id = note.noteId,
                date = date,
                timeLabel = "%02d:%02d".format(createdAt.hour, createdAt.minute),
                type = RecentItemType.NOTE,
                title = note.title.ifBlank { noteUntitled },
                subtitle = note.content.take(80)
            )
        }

        reminders.forEach { reminder ->
            val dt = Instant.ofEpochMilli(reminder.remindAt).atZone(zoneId).toLocalDateTime()
            items += DayActivityItem(
                id = reminder.reminderId,
                date = date,
                timeLabel = "%02d:%02d".format(dt.hour, dt.minute),
                type = RecentItemType.REMINDER,
                title = reminder.title,
                subtitle = reminder.description
            )
        }

        val scheduledDate = date.toStartOfDayMillis(zoneId)
        val executionsByRoutine = executions.associateBy { it.routineId }
        val schedulesByRoutine = schedules.groupBy { it.routineId }
        routines
            .filter { it.isDueOn(date, zoneId) }
            .forEach { routine ->
                val execution = executionsByRoutine[routine.routineId]
                    ?.takeIf { it.scheduledDate == scheduledDate }
                val firstSchedule = schedulesByRoutine[routine.routineId].orEmpty()
                    .minByOrNull { it.timeOfDay }

                items += DayActivityItem(
                    id = routine.routineId,
                    date = date,
                    timeLabel = firstSchedule?.timeOfDay.toTimeLabel(),
                    type = RecentItemType.ROUTINE,
                    title = routine.name,
                    subtitle = "",
                    routineStatus = execution?.status ?: RoutineExecutionStatus.PENDING,
                    routineExecutionId = execution?.executionId
                )
            }

        return items.sortedBy { it.timeLabel }
    }

private fun Int?.toTimeLabel(): String {
    if (this == null) return "--:--"
    val hours = this / 60
    val minutes = this % 60
    return "%02d:%02d".format(hours, minutes)
}
