package com.notea.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val REMINDERS = "reminders"
    const val ROUTINES = "routines"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val reminderChannel = NotificationChannel(
            REMINDERS,
            "Recordatorios",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos locales de recordatorios"
        }

        val routineChannel = NotificationChannel(
            ROUTINES,
            "Rutinas",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos locales de rutinas"
        }

        manager.createNotificationChannels(listOf(reminderChannel, routineChannel))
    }
}
