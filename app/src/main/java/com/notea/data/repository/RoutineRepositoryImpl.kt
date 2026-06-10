package com.notea.data.repository

import com.notea.data.local.dao.RoutineDao
import com.notea.data.local.entity.RoutineEntity
import com.notea.data.local.entity.RoutineScheduleEntity
import com.notea.data.mapper.toDomain
import com.notea.domain.model.RoutineDraft
import com.notea.domain.model.RoutineSummary
import com.notea.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val clock: Clock
) : RoutineRepository {

    override fun observeRoutines(query: String): Flow<List<RoutineSummary>> {
        val source = if (query.isBlank()) routineDao.observeActiveRoutines() else routineDao.searchActiveRoutines(query)
        return source.map { items -> items.map { it.toDomain() } }
    }

    override suspend fun getRoutineById(routineId: Long): RoutineSummary? =
        routineDao.getRoutineById(routineId)?.toDomain()

    override suspend fun upsert(draft: RoutineDraft): Long {
        val now = clock.millis()
        val existing = draft.routineId?.let { routineDao.getRoutineById(it) }
        val entity = if (existing == null) {
            RoutineEntity(
                name = draft.name.trim(),
                description = draft.description?.trim(),
                frequencyType = draft.frequencyType,
                customRule = draft.customRule,
                isPaused = draft.isPaused,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing.copy(
                name = draft.name.trim(),
                description = draft.description?.trim(),
                frequencyType = draft.frequencyType,
                customRule = draft.customRule,
                isPaused = draft.isPaused,
                updatedAt = now
            )
        }
        val insertedId = routineDao.insertRoutine(entity)
        val finalId = draft.routineId ?: insertedId
        routineDao.deleteSchedulesForRoutine(finalId)
        val schedules = draft.scheduleMinutes.distinct().sorted().map {
            RoutineScheduleEntity(routineId = finalId, timeOfDay = it)
        }
        if (schedules.isNotEmpty()) {
            routineDao.insertSchedules(schedules)
        }
        return finalId
    }

    override suspend fun delete(routineId: Long) {
        routineDao.deleteRoutineById(routineId)
    }
}
