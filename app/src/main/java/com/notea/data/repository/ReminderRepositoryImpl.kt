package com.notea.data.repository

import com.notea.data.local.dao.ReminderDao
import com.notea.data.local.entity.ReminderEntity
import com.notea.data.mapper.toDomain
import com.notea.data.scheduler.ReminderScheduler
import com.notea.domain.model.Reminder
import com.notea.domain.model.ReminderDraft
import com.notea.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock
) : ReminderRepository {

    override fun observeActiveReminders(query: String): Flow<List<Reminder>> {
        val source = if (query.isBlank()) reminderDao.observeActiveReminders() else reminderDao.searchActiveReminders(query)
        return source.map { items -> items.map { it.toDomain() } }
    }

    override suspend fun getReminderById(reminderId: Long): Reminder? =
        reminderDao.getById(reminderId)?.toDomain()

    override suspend fun upsert(draft: ReminderDraft): Long {
        val now = clock.millis()
        val remindAtMillis = draft.remindAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val existing = draft.reminderId?.let { reminderDao.getById(it) }

        val entity = if (existing == null) {
            ReminderEntity(
                title = draft.title.trim(),
                description = draft.description.trim(),
                remindAt = remindAtMillis,
                notificationsEnabled = draft.notificationsEnabled,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing.copy(
                title = draft.title.trim(),
                description = draft.description.trim(),
                remindAt = remindAtMillis,
                notificationsEnabled = draft.notificationsEnabled,
                updatedAt = now
            )
        }

        val insertedId = reminderDao.insert(entity)
        val finalId = draft.reminderId ?: insertedId

        if (draft.notificationsEnabled && remindAtMillis > now) {
            reminderScheduler.schedule(finalId, remindAtMillis, entity.title, entity.description)
        } else {
            reminderScheduler.cancel(finalId)
        }

        return finalId
    }

    override suspend fun delete(reminderId: Long) {
        reminderScheduler.cancel(reminderId)
        reminderDao.deleteById(reminderId)
    }

    override suspend fun setCompleted(reminderId: Long, completed: Boolean) {
        val current = reminderDao.getById(reminderId) ?: return
        val updated = current.copy(
            isCompleted = completed,
            completedAt = if (completed) clock.millis() else null,
            updatedAt = clock.millis()
        )
        reminderDao.update(updated)
        if (completed) {
            reminderScheduler.cancel(reminderId)
        } else if (updated.notificationsEnabled && updated.remindAt > clock.millis()) {
            reminderScheduler.schedule(updated.reminderId, updated.remindAt, updated.title, updated.description)
        }
    }

    override suspend fun reschedulePending() {
        val now = clock.millis()
        reminderDao.getPendingForReschedule(now).forEach { reminder ->
            reminderScheduler.schedule(reminder.reminderId, reminder.remindAt, reminder.title, reminder.description)
        }
    }
}
