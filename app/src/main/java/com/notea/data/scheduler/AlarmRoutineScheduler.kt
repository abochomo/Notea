package com.notea.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.notea.data.receiver.RoutineAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AlarmRoutineScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : RoutineScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(
        routineId: Long,
        scheduleMinutes: Int,
        triggerAtMillis: Long,
        scheduledDate: LocalDate
    ) {
        val pendingIntent = pendingIntent(routineId, scheduleMinutes, scheduledDate)
        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canUseExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(routineId: Long, scheduleMinutes: Int) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                requestCode(routineId, scheduleMinutes),
                Intent(context, RoutineAlarmReceiver::class.java).apply {
                    action = RoutineAlarmReceiver.ACTION_ROUTINE_ALARM
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun pendingIntent(
        routineId: Long,
        scheduleMinutes: Int,
        scheduledDate: LocalDate
    ): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = RoutineAlarmReceiver.ACTION_ROUTINE_ALARM
            putExtra(RoutineAlarmReceiver.EXTRA_ROUTINE_ID, routineId)
            putExtra(RoutineAlarmReceiver.EXTRA_SCHEDULE_MINUTES, scheduleMinutes)
            putExtra(RoutineAlarmReceiver.EXTRA_SCHEDULED_DATE, scheduledDate.toString())
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(routineId, scheduleMinutes),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(routineId: Long, scheduleMinutes: Int): Int {
        val raw = 31 * routineId.hashCode() + scheduleMinutes
        return if (raw == Int.MIN_VALUE) 0 else abs(raw)
    }
}
