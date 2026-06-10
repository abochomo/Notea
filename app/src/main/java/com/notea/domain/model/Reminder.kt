package com.notea.domain.model

import java.time.Instant
import java.time.LocalDateTime

data class Reminder(
    val reminderId: Long,
    val title: String,
    val description: String,
    val remindAt: LocalDateTime,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    val notificationsEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ReminderDraft(
    val reminderId: Long? = null,
    val title: String,
    val description: String,
    val remindAt: LocalDateTime,
    val notificationsEnabled: Boolean = true
)
