package com.notea.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notea.domain.repository.ReminderRepository
import com.notea.domain.repository.RoutineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RescheduleAlarmsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val routineRepository: RoutineRepository
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        reminderRepository.reschedulePending()
        routineRepository.rescheduleRoutineNotifications()
        return Result.success()
    }
}
