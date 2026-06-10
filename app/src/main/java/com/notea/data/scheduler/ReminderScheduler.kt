package com.notea.data.scheduler

interface ReminderScheduler {
    fun schedule(reminderId: Long, triggerAtMillis: Long, title: String, description: String)
    fun cancel(reminderId: Long)
}
