package com.notea.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.notea.data.worker.RescheduleAlarmsWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reschedule_alarms",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RescheduleAlarmsWorker>().build()
            )
        }
    }
}
