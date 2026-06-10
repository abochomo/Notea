package com.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notea.data.local.entity.RoutineEntity
import com.notea.data.local.entity.RoutineScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE isPaused = 0 ORDER BY updatedAt DESC")
    fun observeActiveRoutines(): Flow<List<RoutineEntity>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(items: List<RoutineScheduleEntity>)

    @Query("DELETE FROM routine_schedules WHERE routineId = :routineId")
    suspend fun deleteSchedulesForRoutine(routineId: Long)
}
