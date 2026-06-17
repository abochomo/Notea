package com.notea.domain.model

import java.time.Instant
import java.time.LocalDate

data class RoutineSummary(
    val routineId: Long,
    val name: String,
    val description: String?,
    val frequencyType: FrequencyType,
    val customRule: String?,
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

data class RoutineExecution(
    val executionId: Long,
    val routineId: Long,
    val scheduledDate: LocalDate,
    val openedAt: Instant?,
    val answeredAt: Instant?,
    val status: RoutineExecutionStatus
)

data class RoutineAlarmInfo(
    val routineId: Long,
    val routineName: String,
    val date: LocalDate
)

data class RoutineAnswer(
    val answerId: Long,
    val executionId: Long,
    val questionId: Long,
    val valueText: String?,
    val valueNumber: Double?,
    val valueBoolean: Boolean?,
    val valueJson: String?
)

data class RoutineAnswerDraft(
    val questionId: Long,
    val valueText: String? = null,
    val valueNumber: Double? = null,
    val valueBoolean: Boolean? = null,
    val valueJson: String? = null
)

data class RoutineQuestion(
    val questionId: Long,
    val routineId: Long,
    val label: String,
    val fieldType: QuestionFieldType,
    val isRequired: Boolean,
    val position: Int,
    val options: List<RoutineQuestionOption> = emptyList()
)

data class RoutineQuestionDraft(
    val questionId: Long? = null,
    val routineId: Long,
    val label: String,
    val fieldType: QuestionFieldType,
    val isRequired: Boolean = false,
    val position: Int,
    val options: List<RoutineQuestionOptionDraft> = emptyList()
)

data class RoutineQuestionOption(
    val optionId: Long,
    val questionId: Long,
    val text: String,
    val position: Int
)

data class RoutineQuestionOptionDraft(
    val optionId: Long? = null,
    val text: String,
    val position: Int
)
