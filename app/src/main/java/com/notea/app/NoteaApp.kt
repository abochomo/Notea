package com.notea.app

import android.app.Application
import androidx.room.Room
import com.notea.app.data.AppDb
import com.notea.app.data.Prefs
import com.notea.app.data.Repository
import com.notea.app.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NoteaApp : Application() {
    lateinit var db: AppDb
        private set
    lateinit var repo: Repository
        private set
    lateinit var prefs: Prefs
        private set

    /** Ámbito que sobrevive a las pantallas: guardados al salir, reprogramación de alarmas. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDb::class.java, "notea.db").build()
        repo = Repository(this, db)
        prefs = Prefs(this)
        Notifier.createChannels(this)
        scope.launch {
            try {
                Notifier.rescheduleAll(this@NoteaApp)
            } catch (e: Exception) {
                // La reprogramación es una red de seguridad; no debe tumbar el arranque.
            }
        }
    }
}
