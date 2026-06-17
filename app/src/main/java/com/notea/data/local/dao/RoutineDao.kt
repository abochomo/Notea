package com.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notea.data.local.entity.AnswerEntity
import com.notea.data.local.entity.QuestionOptionEntity
import com.notea.data.local.entity.RoutineEntity
import com.notea.data.local.entity.RoutineExecutionEntity
import com.notea.data.local.entity.RoutineQuestionEntity
import com.notea.data.local.entity.RoutineScheduleEntity
import com.notea.domain.model.RoutineExecutionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE isPaused = 0 ORDER BY updatedAt DESC")
    fun observeActiveRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isPaused = 0 ORDER BY updatedAt DESC")
    suspend fun getActiveRoutines(): List<RoutineEntity>

    @Query("""
        SELECT * FROM routines
        WHERE isPaused = 0
          AND createdAt <= :maxCreatedAt
        ORDER BY updatedAt DESC
    """)
    fun observeActiveRoutinesCreatedBefore(maxCreatedAt: Long): Flow<List<RoutineEntity>>

    @Query("""
        SELECT * FROM routines
        WHERE isPaused = 0
          AND name LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchActiveRoutines(query: String): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE routineId = :routineId")
    suspend fun getRoutineById(routineId: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE routineId = :routineId")
    suspend fun deleteRoutineById(routineId: Long)

    @Query("SELECT * FROM routine_schedules WHERE routineId = :routineId ORDER BY timeOfDay ASC")
    suspend fun getSchedulesForRoutine(routineId: Long): List<RoutineScheduleEntity>

    @Query("""
        SELECT * FROM routine_schedules
        WHERE routineId IN (:routineIds)
        ORDER BY routineId ASC, timeOfDay ASC
    """)
    fun observeSchedulesForRoutines(routineIds: List<Long>): Flow<List<RoutineScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(items: List<RoutineScheduleEntity>)

    @Query("DELETE FROM routine_schedules WHERE routineId = :routineId")
    suspend fun deleteSchedulesForRoutine(routineId: Long)

    @Query("SELECT * FROM routine_questions WHERE routineId = :routineId ORDER BY displayOrder ASC, questionId ASC")
    suspend fun getQuestionsForRoutine(routineId: Long): List<RoutineQuestionEntity>

    @Query("SELECT * FROM routine_questions WHERE questionId = :questionId")
    suspend fun getQuestionById(questionId: Long): RoutineQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: RoutineQuestionEntity): Long

    @Update
    suspend fun updateQuestion(question: RoutineQuestionEntity)

    @Query("DELETE FROM routine_questions WHERE questionId = :questionId")
    suspend fun deleteQuestionById(questionId: Long)

    @Query("DELETE FROM routine_questions WHERE routineId = :routineId")
    suspend fun deleteQuestionsForRoutine(routineId: Long)

    @Query("SELECT * FROM question_options WHERE questionId = :questionId ORDER BY displayOrder ASC, optionId ASC")
    suspend fun getOptionsForQuestion(questionId: Long): List<QuestionOptionEntity>

    @Query("SELECT * FROM question_options WHERE questionId IN (:questionIds) ORDER BY questionId ASC, displayOrder ASC, optionId ASC")
    suspend fun getOptionsForQuestions(questionIds: List<Long>): List<QuestionOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(items: List<QuestionOptionEntity>)

    @Update
    suspend fun updateOption(option: QuestionOptionEntity)

    @Query("DELETE FROM question_options WHERE optionId = :optionId")
    suspend fun deleteOptionById(optionId: Long)

    @Query("DELETE FROM question_options WHERE questionId = :questionId")
    suspend fun deleteOptionsForQuestion(questionId: Long)

    @Query("""
        SELECT * FROM routine_executions
        WHERE routineId = :routineId
          AND scheduledDate = :scheduledDate
        LIMIT 1
    """)
    suspend fun getExecutionForRoutineDate(
        routineId: Long,
        scheduledDate: Long
    ): RoutineExecutionEntity?

    @Query("SELECT * FROM routine_executions WHERE executionId = :executionId")
    suspend fun getExecutionById(executionId: Long): RoutineExecutionEntity?

    @Query("""
        SELECT * FROM routine_executions
        WHERE routineId = :routineId
          AND status = :status
        ORDER BY scheduledDate DESC
    """)
    suspend fun getExecutionsForRoutineByStatus(
        routineId: Long,
        status: RoutineExecutionStatus
    ): List<RoutineExecutionEntity>

    @Query("""
        SELECT * FROM routine_executions
        WHERE scheduledDate BETWEEN :from AND :to
        ORDER BY scheduledDate ASC
    """)
    fun observeExecutionsBetween(from: Long, to: Long): Flow<List<RoutineExecutionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExecution(execution: RoutineExecutionEntity): Long

    @Query("""
        UPDATE routine_executions
        SET openedAt = :openedAt
        WHERE executionId = :executionId
          AND openedAt IS NULL
    """)
    suspend fun markExecutionOpenedIfNeeded(
        executionId: Long,
        openedAt: Long
    )

    @Query("""
        UPDATE routine_executions
        SET answeredAt = :answeredAt,
            status = :status
        WHERE executionId = :executionId
    """)
    suspend fun markExecutionAnswered(
        executionId: Long,
        answeredAt: Long,
        status: RoutineExecutionStatus
    )

    @Query("SELECT * FROM answers WHERE executionId = :executionId ORDER BY questionId ASC")
    suspend fun getAnswersForExecution(executionId: Long): List<AnswerEntity>

    @Query("DELETE FROM answers WHERE executionId = :executionId")
    suspend fun deleteAnswersForExecution(executionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(items: List<AnswerEntity>)
}
