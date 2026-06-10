package com.notea.domain.repository

import com.notea.domain.model.RoutineDraft
import com.notea.domain.model.RoutineSummary
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutines(query: String = ""): Flow<List<RoutineSummary>>
    suspend fun getRoutineById(routineId: Long): RoutineSummary?
    suspend fun upsert(draft: RoutineDraft): Long
    suspend fun delete(routineId: Long)
}
