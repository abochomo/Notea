package com.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notea.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY remindAt ASC")
    fun observeActiveReminders(): Flow<List<ReminderEntity>>

    @Query("""
        SELECT * FROM reminders
        WHERE isCompleted = 0
          AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY remindAt ASC
    """)
    fun searchActiveReminders(query: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE reminderId = :reminderId")
    suspend fun getById(reminderId: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE reminderId = :reminderId")
    suspend fun deleteById(reminderId: Long)

    @Query("""
        SELECT * FROM reminders
        WHERE notificationsEnabled = 1
          AND isCompleted = 0
          AND remindAt >= :now
        ORDER BY remindAt ASC
    """)
    suspend fun getPendingForReschedule(now: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE remindAt BETWEEN :from AND :to ORDER BY remindAt ASC")
    suspend fun getActivityBetween(from: Long, to: Long): List<ReminderEntity>
}
