package com.notea.domain.repository

import com.notea.domain.model.CalendarDaySummary
import com.notea.domain.model.DayActivityItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface CalendarRepository {
    fun observeMonth(month: YearMonth): Flow<Map<LocalDate, CalendarDaySummary>>
    fun observeDay(date: LocalDate): Flow<List<DayActivityItem>>
}
