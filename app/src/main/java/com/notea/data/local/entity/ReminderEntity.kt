package com.notea.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["remindAt"]),
        Index(value = ["isCompleted"]),
        Index(value = ["notificationsEnabled"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val reminderId: Long = 0,
    val title: String,
    val description: String,
    val remindAt: Long,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notificationsEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
