package com.notea.data.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notea.MainActivity
import com.notea.R
import com.notea.data.notification.NotificationChannels
import com.notea.domain.model.RoutineAlarmInfo
import com.notea.domain.repository.RoutineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlin.math.abs

@HiltWorker
class RoutineAlarmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val routineRepository: RoutineRepository
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val routineId = inputData.getLong(KEY_ROUTINE_ID, -1L)
        val scheduleMinutes = inputData.getInt(KEY_SCHEDULE_MINUTES, -1)
        val date = inputData.getString(KEY_SCHEDULED_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        if (routineId <= 0L || scheduleMinutes !in 0..1439 || date == null) {
            return Result.success()
        }

        val alarmInfo = routineRepository.getRoutineAlarmInfo(routineId, scheduleMinutes, date)
        if (alarmInfo != null) {
            showNotification(alarmInfo, scheduleMinutes)
        }
        routineRepository.scheduleNextRoutineNotification(routineId, scheduleMinutes)

        return Result.success()
    }

    private fun showNotification(alarmInfo: RoutineAlarmInfo, scheduleMinutes: Int) {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationId = notificationId(alarmInfo.routineId, scheduleMinutes)
        val openIntent = Intent(
            Intent.ACTION_VIEW,
            "notea://routine/${alarmInfo.routineId}/${alarmInfo.date}".toUri(),
            applicationContext,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.ROUTINES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(alarmInfo.routineName)
            .setContentText(applicationContext.getString(R.string.notification_routine_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }

    private fun notificationId(routineId: Long, scheduleMinutes: Int): Int {
        val raw = 31 * routineId.hashCode() + scheduleMinutes
        return if (raw == Int.MIN_VALUE) 0 else abs(raw)
    }

    companion object {
        const val KEY_ROUTINE_ID = "routine_id"
        const val KEY_SCHEDULE_MINUTES = "schedule_minutes"
        const val KEY_SCHEDULED_DATE = "scheduled_date"
    }
}
