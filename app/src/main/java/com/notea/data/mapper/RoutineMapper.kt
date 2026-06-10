package com.notea.data.mapper

import com.notea.data.local.entity.RoutineEntity
import com.notea.domain.model.RoutineSummary
import java.time.Instant

fun RoutineEntity.toDomain(): RoutineSummary = RoutineSummary(
    routineId = routineId,
    name = name,
    description = description,
    frequencyType = frequencyType,
    isPaused = isPaused,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)
