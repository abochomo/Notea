package com.notea.data.repository

import com.notea.data.local.dao.NoteDao
import com.notea.data.local.dao.ReminderDao
import com.notea.domain.model.CalendarDaySummary
import com.notea.domain.model.DayActivityItem
import com.notea.domain.model.RecentItemType
import com.notea.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao
) : CalendarRepository {

    override fun observeMonth(month: YearMonth): Flow<Map<LocalDate, CalendarDaySummary>> = flow {
        val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val map = mutableMapOf<LocalDate, CalendarDaySummary>()

        noteDao.getActivityBetween(start, end).forEach { note ->
            val createdDay = Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            map[createdDay] = (map[createdDay] ?: CalendarDaySummary()).copy(
                notes = (map[createdDay]?.notes ?: 0) + 1
            )
            val updatedDay = Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            if (updatedDay != createdDay) {
                map[updatedDay] = (map[updatedDay] ?: CalendarDaySummary()).copy(
                    notes = (map[updatedDay]?.notes ?: 0) + 1
                )
            }
        }

        reminderDao.getActivityBetween(start, end).forEach { reminder ->
            val day = Instant.ofEpochMilli(reminder.remindAt).atZone(ZoneId.systemDefault()).toLocalDate()
            map[day] = (map[day] ?: CalendarDaySummary()).copy(
                reminders = (map[day]?.reminders ?: 0) + 1
            )
        }

        emit(map)
    }

    override fun observeDay(date: LocalDate): Flow<List<DayActivityItem>> = flow {
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val items = mutableListOf<DayActivityItem>()

        noteDao.getActivityBetween(start, end).forEach { note ->
            val updatedAt = Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
            items += DayActivityItem(
                date = date,
                timeLabel = "%02d:%02d".format(updatedAt.hour, updatedAt.minute),
                type = RecentItemType.NOTE,
                title = note.title.ifBlank { "Nota sin título" },
                subtitle = note.content.take(80)
            )
        }

        reminderDao.getActivityBetween(start, end).forEach { reminder ->
            val dt = Instant.ofEpochMilli(reminder.remindAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
            items += DayActivityItem(
                date = date,
                timeLabel = "%02d:%02d".format(dt.hour, dt.minute),
                type = RecentItemType.REMINDER,
                title = reminder.title,
                subtitle = reminder.description
            )
        }

        emit(items.sortedBy { it.timeLabel })
    }
}
