package com.notea.data.mapper

import com.notea.data.local.entity.ReminderEntity
import com.notea.domain.model.Reminder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun ReminderEntity.toDomain(): Reminder = Reminder(
    reminderId = reminderId,
    title = title,
    description = description,
    remindAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(remindAt), ZoneId.systemDefault()),
    isCompleted = isCompleted,
    completedAt = completedAt?.let(Instant::ofEpochMilli),
    notificationsEnabled = notificationsEnabled,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)
