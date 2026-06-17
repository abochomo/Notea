package com.notea.data.repository

import androidx.room.withTransaction
import com.notea.data.local.dao.RoutineDao
import com.notea.data.local.db.NoteaDatabase
import com.notea.data.local.entity.AnswerEntity
import com.notea.data.local.entity.QuestionOptionEntity
import com.notea.data.local.entity.RoutineEntity
import com.notea.data.local.entity.RoutineExecutionEntity
import com.notea.data.local.entity.RoutineQuestionEntity
import com.notea.data.local.entity.RoutineScheduleEntity
import com.notea.data.mapper.toDomain
import com.notea.data.scheduler.RoutineScheduler
import com.notea.domain.model.FrequencyType
import com.notea.domain.model.QuestionFieldType
import com.notea.domain.model.RoutineAnswer
import com.notea.domain.model.RoutineAnswerDraft
import com.notea.domain.model.RoutineAlarmInfo
import com.notea.domain.model.RoutineDraft
import com.notea.domain.model.RoutineExecution
import com.notea.domain.model.RoutineExecutionStatus
import com.notea.domain.model.RoutineQuestion
import com.notea.domain.model.RoutineQuestionDraft
import com.notea.domain.model.RoutineSummary
import com.notea.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val database: NoteaDatabase,
    private val routineDao: RoutineDao,
    private val routineScheduler: RoutineScheduler,
    private val clock: Clock
) : RoutineRepository {

    override fun observeRoutines(query: String): Flow<List<RoutineSummary>> {
        val source = if (query.isBlank()) routineDao.observeActiveRoutines() else routineDao.searchActiveRoutines(query)
        return source.map { items -> items.map { it.toDomain() } }
    }

    override suspend fun getRoutineById(routineId: Long): RoutineSummary? =
        routineDao.getRoutineById(routineId)?.toDomain()

    override suspend fun getOrCreateExecution(
        routineId: Long,
        date: LocalDate
    ): RoutineExecution? =
        database.withTransaction {
            val routine = routineDao.getRoutineById(routineId) ?: return@withTransaction null
            if (!routine.isDueOn(date, clock.zone)) return@withTransaction null

            val scheduledDate = date.toStartOfDayMillis(clock.zone)
            val existing = routineDao.getExecutionForRoutineDate(routineId, scheduledDate)
            val execution = existing ?: run {
                val newExecution = RoutineExecutionEntity(
                    routineId = routineId,
                    scheduledDate = scheduledDate,
                    openedAt = clock.millis()
                )
                val insertedId = routineDao.insertExecution(newExecution)
                if (insertedId > 0L) {
                    newExecution.copy(executionId = insertedId)
                } else {
                    routineDao.getExecutionForRoutineDate(routineId, scheduledDate)
                        ?: error("No se pudo crear la ejecucion de la rutina.")
                }
            }

            val openedExecution = if (execution.openedAt == null) {
                val openedAt = clock.millis()
                routineDao.markExecutionOpenedIfNeeded(execution.executionId, openedAt)
                execution.copy(openedAt = openedAt)
            } else {
                execution
            }

            openedExecution.toDomain(clock.zone)
        }

    override suspend fun getCompletedExecutionsForRoutine(routineId: Long): List<RoutineExecution> =
        routineDao.getExecutionsForRoutineByStatus(
            routineId = routineId,
            status = RoutineExecutionStatus.ANSWERED
        ).map { it.toDomain(clock.zone) }

    override suspend fun getRoutineAlarmInfo(
        routineId: Long,
        scheduleMinutes: Int,
        date: LocalDate
    ): RoutineAlarmInfo? {
        if (scheduleMinutes !in 0..1439) return null

        val routine = routineDao.getRoutineById(routineId) ?: return null
        if (!routine.isDueOn(date, clock.zone)) return null

        val hasSchedule = routineDao.getSchedulesForRoutine(routineId)
            .any { it.isEnabled && it.timeOfDay == scheduleMinutes }
        if (!hasSchedule) return null

        return RoutineAlarmInfo(
            routineId = routine.routineId,
            routineName = routine.name,
            date = date
        )
    }

    override suspend fun scheduleNextRoutineNotification(routineId: Long, scheduleMinutes: Int) {
        val routine = routineDao.getRoutineById(routineId)
        if (routine == null || routine.isPaused || scheduleMinutes !in 0..1439) {
            routineScheduler.cancel(routineId, scheduleMinutes)
            return
        }

        val hasSchedule = routineDao.getSchedulesForRoutine(routineId)
            .any { it.isEnabled && it.timeOfDay == scheduleMinutes }
        if (!hasSchedule) {
            routineScheduler.cancel(routineId, scheduleMinutes)
            return
        }

        val next = routine.nextTrigger(scheduleMinutes, clock)
        if (next == null) {
            routineScheduler.cancel(routineId, scheduleMinutes)
        } else {
            routineScheduler.schedule(
                routineId = routineId,
                scheduleMinutes = scheduleMinutes,
                triggerAtMillis = next.triggerAtMillis,
                scheduledDate = next.date
            )
        }
    }

    override suspend fun rescheduleRoutineNotifications() {
        routineDao.getActiveRoutines().forEach { routine ->
            routineDao.getSchedulesForRoutine(routine.routineId)
                .filter { it.isEnabled }
                .forEach { schedule ->
                    scheduleNextRoutineNotification(routine.routineId, schedule.timeOfDay)
                }
        }
    }

    override suspend fun getAnswersForExecution(executionId: Long): List<RoutineAnswer> =
        routineDao.getAnswersForExecution(executionId).map { it.toDomain() }

    override suspend fun saveExecutionAnswers(
        executionId: Long,
        answers: List<RoutineAnswerDraft>
    ): RoutineExecution? =
        database.withTransaction {
            val execution = routineDao.getExecutionById(executionId) ?: return@withTransaction null
            val validQuestionIds = routineDao.getQuestionsForRoutine(execution.routineId)
                .map { it.questionId }
                .toSet()
            val normalizedAnswers = answers
                .filter { it.questionId in validQuestionIds }
                .distinctBy { it.questionId }

            routineDao.deleteAnswersForExecution(executionId)
            if (normalizedAnswers.isNotEmpty()) {
                routineDao.insertAnswers(
                    normalizedAnswers.map { draft ->
                        AnswerEntity(
                            executionId = executionId,
                            questionId = draft.questionId,
                            valueText = draft.valueText,
                            valueNumber = draft.valueNumber,
                            valueBoolean = draft.valueBoolean,
                            valueJson = draft.valueJson
                        )
                    }
                )
            }

            val answeredAt = clock.millis()
            routineDao.markExecutionAnswered(
                executionId = executionId,
                answeredAt = answeredAt,
                status = RoutineExecutionStatus.ANSWERED
            )
            routineDao.getExecutionById(executionId)?.toDomain(clock.zone)
        }

    override suspend fun getScheduleMinutesForRoutine(routineId: Long): List<Int> =
        routineDao.getSchedulesForRoutine(routineId).map { it.timeOfDay }

    override suspend fun getQuestionsForRoutine(routineId: Long): List<RoutineQuestion> =
        database.withTransaction {
            val questions = routineDao.getQuestionsForRoutine(routineId)
            val questionIds = questions.map { it.questionId }
            val optionsByQuestionId = if (questionIds.isEmpty()) {
                emptyMap()
            } else {
                routineDao.getOptionsForQuestions(questionIds).groupBy { it.questionId }
            }

            questions.map { question ->
                question.toDomain(optionsByQuestionId[question.questionId].orEmpty())
            }
        }

    override suspend fun upsertQuestion(draft: RoutineQuestionDraft): Long =
        database.withTransaction {
            val normalizedDraft = draft.toValidatedQuestionDraft()
            val existing = draft.questionId?.let { routineDao.getQuestionById(it) }
            val questionId = if (existing == null) {
                routineDao.insertQuestion(
                    RoutineQuestionEntity(
                        routineId = normalizedDraft.routineId,
                        label = normalizedDraft.label,
                        fieldType = normalizedDraft.fieldType,
                        isRequired = normalizedDraft.isRequired,
                        displayOrder = normalizedDraft.position
                    )
                )
            } else {
                routineDao.updateQuestion(
                    existing.copy(
                        label = normalizedDraft.label,
                        fieldType = normalizedDraft.fieldType,
                        isRequired = normalizedDraft.isRequired,
                        displayOrder = normalizedDraft.position
                    )
                )
                existing.questionId
            }

            routineDao.deleteOptionsForQuestion(questionId)
            insertQuestionOptions(questionId, normalizedDraft.options)
            questionId
        }

    override suspend fun replaceQuestionsForRoutine(
        routineId: Long,
        questions: List<RoutineQuestionDraft>
    ) {
        database.withTransaction {
            routineDao.deleteQuestionsForRoutine(routineId)
            questions
                .sortedBy { it.position }
                .forEachIndexed { index, draft ->
                    val normalizedDraft = draft.copy(
                        questionId = null,
                        routineId = routineId,
                        position = index
                    ).toValidatedQuestionDraft()
                    val questionId = routineDao.insertQuestion(
                        RoutineQuestionEntity(
                            routineId = routineId,
                            label = normalizedDraft.label,
                            fieldType = normalizedDraft.fieldType,
                            isRequired = normalizedDraft.isRequired,
                            displayOrder = normalizedDraft.position
                        )
                    )
                    insertQuestionOptions(questionId, normalizedDraft.options)
                }
        }
    }

    override suspend fun deleteQuestion(questionId: Long) {
        routineDao.deleteQuestionById(questionId)
    }

    override suspend fun upsert(draft: RoutineDraft): Long {
        val result = database.withTransaction {
            val now = clock.millis()
            val existing = draft.routineId?.let { routineDao.getRoutineById(it) }
            val oldScheduleMinutes = existing
                ?.let { routineDao.getSchedulesForRoutine(it.routineId).map { schedule -> schedule.timeOfDay } }
                .orEmpty()
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
            val finalId = entity.routineId.takeIf { it > 0L } ?: insertedId
            routineDao.deleteSchedulesForRoutine(finalId)
            val schedules = draft.scheduleMinutes.distinct().sorted().map {
                RoutineScheduleEntity(routineId = finalId, timeOfDay = it)
            }
            if (schedules.isNotEmpty()) {
                routineDao.insertSchedules(schedules)
            }
            RoutineScheduleUpdate(
                routineId = finalId,
                oldScheduleMinutes = oldScheduleMinutes,
                newScheduleMinutes = schedules.map { it.timeOfDay },
                isPaused = draft.isPaused
            )
        }

        result.oldScheduleMinutes.forEach { routineScheduler.cancel(result.routineId, it) }
        if (!result.isPaused) {
            result.newScheduleMinutes.forEach { scheduleMinutes ->
                scheduleNextRoutineNotification(result.routineId, scheduleMinutes)
            }
        }
        return result.routineId
    }

    override suspend fun delete(routineId: Long) {
        routineDao.getSchedulesForRoutine(routineId).forEach { schedule ->
            routineScheduler.cancel(routineId, schedule.timeOfDay)
        }
        routineDao.deleteRoutineById(routineId)
    }

    private suspend fun insertQuestionOptions(
        questionId: Long,
        options: List<ValidatedQuestionOptionDraft>
    ) {
        if (options.isEmpty()) return

        routineDao.insertOptions(
            options.map {
                QuestionOptionEntity(
                    questionId = questionId,
                    optionText = it.text,
                    displayOrder = it.position
                )
            }
        )
    }
}

private val supportedQuestionTypes = setOf(
    QuestionFieldType.TEXT,
    QuestionFieldType.NUMBER,
    QuestionFieldType.BOOLEAN,
    QuestionFieldType.SINGLE_CHOICE,
    QuestionFieldType.MULTIPLE_CHOICE
)

private data class ValidatedQuestionDraft(
    val routineId: Long,
    val label: String,
    val fieldType: QuestionFieldType,
    val isRequired: Boolean,
    val position: Int,
    val options: List<ValidatedQuestionOptionDraft>
)

private data class ValidatedQuestionOptionDraft(
    val text: String,
    val position: Int
)

private data class RoutineScheduleUpdate(
    val routineId: Long,
    val oldScheduleMinutes: List<Int>,
    val newScheduleMinutes: List<Int>,
    val isPaused: Boolean
)

private data class ScheduledRoutineTrigger(
    val date: LocalDate,
    val triggerAtMillis: Long
)

private fun RoutineQuestionDraft.toValidatedQuestionDraft(): ValidatedQuestionDraft {
    val cleanLabel = label.trim()
    require(cleanLabel.isNotBlank()) { "La pregunta necesita un texto." }
    require(fieldType in supportedQuestionTypes) { "Tipo de pregunta no soportado." }

    val cleanOptions = if (fieldType.hasOptions()) {
        options
            .sortedBy { it.position }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { index, text -> ValidatedQuestionOptionDraft(text, index) }
            .also {
                require(it.size >= 2) {
                    "Una pregunta de selección necesita al menos dos opciones."
                }
            }
    } else {
        emptyList()
    }

    return ValidatedQuestionDraft(
        routineId = routineId,
        label = cleanLabel,
        fieldType = fieldType,
        isRequired = isRequired,
        position = position.coerceAtLeast(0),
        options = cleanOptions
    )
}

private fun QuestionFieldType.hasOptions(): Boolean =
    this == QuestionFieldType.SINGLE_CHOICE || this == QuestionFieldType.MULTIPLE_CHOICE

internal fun RoutineEntity.isDueOn(date: LocalDate, zoneId: ZoneId): Boolean {
    if (isPaused) return false

    val createdDate = Instant.ofEpochMilli(createdAt).atZone(zoneId).toLocalDate()
    if (date < createdDate) return false

    return when (frequencyType) {
        FrequencyType.DAILY -> true
        FrequencyType.WEEKLY -> date.dayOfWeek.value in parseWeeklyDays(customRule)
        FrequencyType.MONTHLY,
        FrequencyType.CUSTOM -> false
    }
}

internal fun LocalDate.toStartOfDayMillis(zoneId: ZoneId): Long =
    atStartOfDay(zoneId).toInstant().toEpochMilli()

private fun RoutineEntity.nextTrigger(
    scheduleMinutes: Int,
    clock: Clock
): ScheduledRoutineTrigger? {
    val zoneId = clock.zone
    val now = clock.millis()
    val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()

    repeat(371) { offset ->
        val date = today.plusDays(offset.toLong())
        if (isDueOn(date, zoneId)) {
            val triggerAt = date.atStartOfDay(zoneId)
                .plusMinutes(scheduleMinutes.toLong())
                .toInstant()
                .toEpochMilli()
            if (triggerAt > now) {
                return ScheduledRoutineTrigger(date, triggerAt)
            }
        }
    }

    return null
}

private fun parseWeeklyDays(value: String?): Set<Int> =
    value.orEmpty()
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .toSet()
