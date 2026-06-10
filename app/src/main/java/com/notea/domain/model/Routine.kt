package com.notea.domain.model

import java.time.Instant

data class RoutineSummary(
    val routineId: Long,
    val name: String,
    val description: String?,
    val frequencyType: FrequencyType,
    val isPaused: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class RoutineDraft(
    val routineId: Long? = null,
    val name: String,
    val description: String?,
    val frequencyType: FrequencyType,
    val customRule: String? = null,
    val scheduleMinutes: List<Int> = emptyList(),
    val isPaused: Boolean = false
)
