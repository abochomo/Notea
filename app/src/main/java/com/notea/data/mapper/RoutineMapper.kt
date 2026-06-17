package com.notea.data.mapper

import com.notea.data.local.entity.QuestionOptionEntity
import com.notea.data.local.entity.AnswerEntity
import com.notea.data.local.entity.RoutineEntity
import com.notea.data.local.entity.RoutineExecutionEntity
import com.notea.data.local.entity.RoutineQuestionEntity
import com.notea.domain.model.RoutineAnswer
import com.notea.domain.model.RoutineExecution
import com.notea.domain.model.RoutineQuestion
import com.notea.domain.model.RoutineQuestionOption
import com.notea.domain.model.RoutineSummary
import java.time.Instant
import java.time.ZoneId

fun RoutineEntity.toDomain(): RoutineSummary = RoutineSummary(
    routineId = routineId,
    name = name,
    description = description,
    frequencyType = frequencyType,
    customRule = customRule,
    isPaused = isPaused,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

fun RoutineQuestionEntity.toDomain(options: List<QuestionOptionEntity>): RoutineQuestion =
    RoutineQuestion(
        questionId = questionId,
        routineId = routineId,
        label = label,
        fieldType = fieldType,
        isRequired = isRequired,
        position = displayOrder,
        options = options.map { it.toDomain() }
    )

fun QuestionOptionEntity.toDomain(): RoutineQuestionOption =
    RoutineQuestionOption(
        optionId = optionId,
        questionId = questionId,
        text = optionText,
        position = displayOrder
    )

fun RoutineExecutionEntity.toDomain(zoneId: ZoneId): RoutineExecution =
    RoutineExecution(
        executionId = executionId,
        routineId = routineId,
        scheduledDate = Instant.ofEpochMilli(scheduledDate).atZone(zoneId).toLocalDate(),
        openedAt = openedAt?.let(Instant::ofEpochMilli),
        answeredAt = answeredAt?.let(Instant::ofEpochMilli),
        status = status
    )

fun AnswerEntity.toDomain(): RoutineAnswer =
    RoutineAnswer(
        answerId = answerId,
        executionId = executionId,
        questionId = questionId,
        valueText = valueText,
        valueNumber = valueNumber,
        valueBoolean = valueBoolean,
        valueJson = valueJson
    )
