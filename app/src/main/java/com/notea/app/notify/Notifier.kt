package com.notea.app.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.notea.app.MainActivity
import com.notea.app.NoteaApp
import com.notea.app.R
import com.notea.app.data.Note
import com.notea.app.data.Routine
import com.notea.app.data.nextOccurrence
import com.notea.app.data.occursOn
import com.notea.app.data.toMillis
import com.notea.app.md.plainSnippet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Programación de alarmas y publicación de notificaciones. */
object Notifier {
    const val CH_REMINDERS = "reminders"
    const val CH_ROUTINES = "routines"

    const val ACTION_NOTE = "com.notea.app.ALARM_NOTE"
    const val ACTION_ROUTINE = "com.notea.app.ALARM_ROUTINE"
    const val ACTION_DONE = "com.notea.app.REMINDER_DONE"
    const val ACTION_SNOOZE = "com.notea.app.REMINDER_SNOOZE"

    const val EXTRA_ID = "id"
    const val EXTRA_OPEN_NOTE = "open_note"
    const val EXTRA_OPEN_ROUTINE = "open_routine"
    const val EXTRA_DAY = "day"

    const val SNOOZE_MINUTES = 15L

    private const val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val reminders = NotificationChannel(CH_REMINDERS, "Recordatorios", NotificationManager.IMPORTANCE_HIGH)
        reminders.description = "Avisos de las notas con recordatorio"
        reminders.enableVibration(true)
        val routines = NotificationChannel(CH_ROUTINES, "Rutinas", NotificationManager.IMPORTANCE_DEFAULT)
        routines.description = "Avisos para rellenar tus rutinas"
        nm.createNotificationChannel(reminders)
        nm.createNotificationChannel(routines)
    }

    fun canNotify(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun canScheduleExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun noteNotificationId(id: Long): Int = (id % 1_000_000L).toInt() + 1
    private fun routineNotificationId(id: Long): Int = 1_000_000 + (id % 1_000_000L).toInt()

    private fun alarmIntent(ctx: Context, action: String, id: Long): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("notea://alarm/$action/$id"))
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(ctx, (id % Int.MAX_VALUE).toInt(), intent, PI_FLAGS)
    }

    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (canScheduleExact(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAlarm(ctx: Context, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pi)
    }

    // ---- Recordatorios ----

    fun scheduleNote(ctx: Context, note: Note) {
        val pi = alarmIntent(ctx, ACTION_NOTE, note.id)
        cancelAlarm(ctx, pi)
        val at = note.reminderAt ?: return
        if (!note.isReminder || note.reminderDone) {
            NotificationManagerCompat.from(ctx).cancel(noteNotificationId(note.id))
            return
        }
        if (at <= System.currentTimeMillis()) return
        setAlarm(ctx, at, pi)
    }

    fun cancelNote(ctx: Context, id: Long) {
        cancelAlarm(ctx, alarmIntent(ctx, ACTION_NOTE, id))
        NotificationManagerCompat.from(ctx).cancel(noteNotificationId(id))
    }

    fun dismissNote(ctx: Context, id: Long) {
        NotificationManagerCompat.from(ctx).cancel(noteNotificationId(id))
    }

    fun showNote(ctx: Context, note: Note) {
        val open = Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("notea://open/note/${note.id}"))
            .putExtra(EXTRA_OPEN_NOTE, note.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val content = PendingIntent.getActivity(ctx, noteNotificationId(note.id), open, PI_FLAGS)

        val done = PendingIntent.getBroadcast(
            ctx,
            noteNotificationId(note.id),
            Intent(ctx, ActionReceiver::class.java)
                .setAction(ACTION_DONE)
                .setData(Uri.parse("notea://action/done/${note.id}"))
                .putExtra(EXTRA_ID, note.id),
            PI_FLAGS,
        )
        val snooze = PendingIntent.getBroadcast(
            ctx,
            noteNotificationId(note.id),
            Intent(ctx, ActionReceiver::class.java)
                .setAction(ACTION_SNOOZE)
                .setData(Uri.parse("notea://action/snooze/${note.id}"))
                .putExtra(EXTRA_ID, note.id),
            PI_FLAGS,
        )

        val body = plainSnippet(note.content, 6)
        val builder = NotificationCompat.Builder(ctx, CH_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(note.title.ifBlank { "Recordatorio" })
            .setContentIntent(content)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(0, "Hecho", done)
            .addAction(0, "Posponer $SNOOZE_MINUTES min", snooze)
        if (body.isNotBlank()) {
            builder.setContentText(body.lineSequence().first())
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }
        notifySafely(ctx, noteNotificationId(note.id), builder)
    }

    // ---- Rutinas ----

    fun scheduleRoutine(ctx: Context, routine: Routine) {
        val pi = alarmIntent(ctx, ACTION_ROUTINE, routine.id)
        cancelAlarm(ctx, pi)
        if (!routine.active || !routine.notify) return
        val next = routine.nextOccurrence(LocalDateTime.now()) ?: return
        setAlarm(ctx, next.toMillis(), pi)
    }

    fun cancelRoutine(ctx: Context, id: Long) {
        cancelAlarm(ctx, alarmIntent(ctx, ACTION_ROUTINE, id))
        dismissRoutine(ctx, id)
    }

    fun dismissRoutine(ctx: Context, id: Long) {
        NotificationManagerCompat.from(ctx).cancel(routineNotificationId(id))
    }

    fun showRoutine(ctx: Context, routine: Routine, day: Long) {
        val open = Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("notea://open/routine/${routine.id}/$day"))
            .putExtra(EXTRA_OPEN_ROUTINE, routine.id)
            .putExtra(EXTRA_DAY, day)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val content = PendingIntent.getActivity(ctx, routineNotificationId(routine.id), open, PI_FLAGS)
        val text = if (routine.isForm) "Toca para responder el cuestionario de hoy" else "Toca para escribir la entrada de hoy"
        val builder = NotificationCompat.Builder(ctx, CH_ROUTINES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(routine.title.ifBlank { "Rutina" })
            .setContentText(text)
            .setContentIntent(content)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        notifySafely(ctx, routineNotificationId(routine.id), builder)
    }

    fun showTest(ctx: Context) {
        val open = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val builder = NotificationCompat.Builder(ctx, CH_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Notea")
            .setContentText("Las notificaciones funcionan correctamente")
            .setContentIntent(PendingIntent.getActivity(ctx, 0, open, PI_FLAGS))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notifySafely(ctx, 999_999_999, builder)
    }

    private fun notifySafely(ctx: Context, id: Int, builder: NotificationCompat.Builder) {
        try {
            if (canNotify(ctx)) NotificationManagerCompat.from(ctx).notify(id, builder.build())
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones: no hay nada que mostrar.
        }
    }

    /**
     * Reprograma todas las alarmas (arranque del dispositivo, inicio de la app, importación)
     * y lanza los recordatorios que vencieron mientras el dispositivo estaba apagado.
     */
    suspend fun rescheduleAll(ctx: Context) {
        val app = ctx.applicationContext as NoteaApp
        val now = System.currentTimeMillis()
        for (note in app.db.notes().getAll()) {
            val at = note.reminderAt
            if (!note.isReminder || at == null || note.reminderDone) continue
            if (at > now) {
                scheduleNote(ctx, note)
            } else if (note.notifiedAt == null) {
                showNote(ctx, note)
                app.db.notes().update(note.copy(notifiedAt = now))
            }
        }
        for (routine in app.db.routines().getAll()) {
            scheduleRoutine(ctx, routine)
        }
    }
}

/** Recibe las alarmas programadas y publica la notificación correspondiente. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as NoteaApp
        val id = intent.getLongExtra(Notifier.EXTRA_ID, 0L)
        val action = intent.action
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == Notifier.ACTION_NOTE) {
                    val note = app.db.notes().get(id)
                    if (note != null && note.isReminder && !note.reminderDone) {
                        Notifier.showNote(app, note)
                        app.db.notes().update(note.copy(notifiedAt = System.currentTimeMillis()))
                    }
                } else if (action == Notifier.ACTION_ROUTINE) {
                    val routine = app.db.routines().get(id)
                    if (routine != null) {
                        val today = LocalDate.now()
                        if (routine.active && routine.notify && routine.occursOn(today) &&
                            app.db.entries().get(id, today.toEpochDay()) == null
                        ) {
                            Notifier.showRoutine(app, routine, today.toEpochDay())
                        }
                        Notifier.scheduleRoutine(app, routine)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Acciones de la notificación de un recordatorio: «Hecho» y «Posponer». */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as NoteaApp
        val id = intent.getLongExtra(Notifier.EXTRA_ID, 0L)
        val action = intent.action
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val note = app.db.notes().get(id)
                Notifier.dismissNote(app, id)
                if (note != null) {
                    val now = System.currentTimeMillis()
                    if (action == Notifier.ACTION_DONE) {
                        app.repo.saveNote(note.copy(reminderDone = true, updatedAt = now))
                    } else if (action == Notifier.ACTION_SNOOZE) {
                        app.repo.saveNote(
                            note.copy(
                                reminderAt = now + Notifier.SNOOZE_MINUTES * 60_000L,
                                notifiedAt = null,
                                updatedAt = now,
                            )
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Tras reiniciar el dispositivo o cambiar la hora, las alarmas se pierden: se reprograman. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Notifier.rescheduleAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
