package com.notea.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.notea.R

object NotificationChannels {
    const val REMINDERS = "reminders"
    const val ROUTINES = "routines"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val reminderChannel = NotificationChannel(
            REMINDERS,
            context.getString(R.string.notification_channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
        }

        val routineChannel = NotificationChannel(
            ROUTINES,
            context.getString(R.string.notification_channel_routines_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_routines_description)
        }

        manager.createNotificationChannels(listOf(reminderChannel, routineChannel))
    }
}
