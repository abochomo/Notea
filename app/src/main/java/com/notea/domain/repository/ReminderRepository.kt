package com.notea.domain.repository

import com.notea.domain.model.Reminder
import com.notea.domain.model.ReminderDraft
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeActiveReminders(query: String = ""): Flow<List<Reminder>>
    suspend fun getReminderById(reminderId: Long): Reminder?
    suspend fun upsert(draft: ReminderDraft): Long
    suspend fun delete(reminderId: Long)
    suspend fun setCompleted(reminderId: Long, completed: Boolean)
    suspend fun reschedulePending()
}
