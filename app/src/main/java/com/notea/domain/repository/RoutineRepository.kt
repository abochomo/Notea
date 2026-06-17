package com.notea.domain.repository

import com.notea.domain.model.RoutineDraft
import com.notea.domain.model.RoutineAnswer
import com.notea.domain.model.RoutineAnswerDraft
import com.notea.domain.model.RoutineAlarmInfo
import com.notea.domain.model.RoutineExecution
import com.notea.domain.model.RoutineQuestion
import com.notea.domain.model.RoutineQuestionDraft
import com.notea.domain.model.RoutineSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RoutineRepository {
    fun observeRoutines(query: String = ""): Flow<List<RoutineSummary>>
    suspend fun getRoutineById(routineId: Long): RoutineSummary?
    suspend fun getOrCreateExecution(routineId: Long, date: LocalDate): RoutineExecution?
    suspend fun getCompletedExecutionsForRoutine(routineId: Long): List<RoutineExecution>
    suspend fun getRoutineAlarmInfo(
        routineId: Long,
        scheduleMinutes: Int,
        date: LocalDate
    ): RoutineAlarmInfo?
    suspend fun scheduleNextRoutineNotification(routineId: Long, scheduleMinutes: Int)
    suspend fun rescheduleRoutineNotifications()
    suspend fun getAnswersForExecution(executionId: Long): List<RoutineAnswer>
    suspend fun saveExecutionAnswers(
        executionId: Long,
        answers: List<RoutineAnswerDraft>
    ): RoutineExecution?
    suspend fun getScheduleMinutesForRoutine(routineId: Long): List<Int>
    suspend fun getQuestionsForRoutine(routineId: Long): List<RoutineQuestion>
    suspend fun upsertQuestion(draft: RoutineQuestionDraft): Long
    suspend fun replaceQuestionsForRoutine(routineId: Long, questions: List<RoutineQuestionDraft>)
    suspend fun deleteQuestion(questionId: Long)
    suspend fun upsert(draft: RoutineDraft): Long
    suspend fun delete(routineId: Long)
}
