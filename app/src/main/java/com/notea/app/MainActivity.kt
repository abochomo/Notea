package com.notea.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.notea.app.notify.Notifier
import com.notea.app.ui.NoteaRoot

/** Destino al que hay que navegar al abrir la app desde una notificación. */
sealed class OpenTarget {
    data class OpenNote(val id: Long) : OpenTarget()
    data class OpenRoutine(val id: Long, val day: Long) : OpenTarget()
}

class MainActivity : ComponentActivity() {
    private val pendingTarget = mutableStateOf<OpenTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingTarget.value = parse(intent)
        setContent {
            NoteaRoot(pendingTarget)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTarget.value = parse(intent)
    }

    private fun parse(intent: Intent?): OpenTarget? {
        if (intent == null) return null
        val noteId = intent.getLongExtra(Notifier.EXTRA_OPEN_NOTE, 0L)
        if (noteId > 0L) return OpenTarget.OpenNote(noteId)
        val routineId = intent.getLongExtra(Notifier.EXTRA_OPEN_ROUTINE, 0L)
        if (routineId > 0L) {
            return OpenTarget.OpenRoutine(routineId, intent.getLongExtra(Notifier.EXTRA_DAY, -1L))
        }
        return null
    }
}
