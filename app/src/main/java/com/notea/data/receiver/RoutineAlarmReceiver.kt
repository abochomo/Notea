package com.notea.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.notea.data.worker.RoutineAlarmWorker

class RoutineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ROUTINE_ALARM) return

        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        val scheduleMinutes = intent.getIntExtra(EXTRA_SCHEDULE_MINUTES, -1)
        val scheduledDate = intent.getStringExtra(EXTRA_SCHEDULED_DATE).orEmpty()
        if (routineId <= 0L || scheduleMinutes !in 0..1439 || scheduledDate.isBlank()) return

        val input = Data.Builder()
            .putLong(RoutineAlarmWorker.KEY_ROUTINE_ID, routineId)
            .putInt(RoutineAlarmWorker.KEY_SCHEDULE_MINUTES, scheduleMinutes)
            .putString(RoutineAlarmWorker.KEY_SCHEDULED_DATE, scheduledDate)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "routine_alarm_${routineId}_${scheduleMinutes}_$scheduledDate",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RoutineAlarmWorker>()
                .setInputData(input)
                .build()
        )
    }

    companion object {
        const val ACTION_ROUTINE_ALARM = "com.notea.action.ROUTINE_ALARM"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_SCHEDULE_MINUTES = "extra_schedule_minutes"
        const val EXTRA_SCHEDULED_DATE = "extra_scheduled_date"
    }
}
