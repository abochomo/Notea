package com.notea.app.data

import android.content.Context
import androidx.room.withTransaction
import com.notea.app.notify.Notifier
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Punto único de acceso a los datos; mantiene las alarmas sincronizadas con lo guardado. */
class Repository(private val ctx: Context, private val db: AppDb) {

    val notes: Flow<List<Note>> = db.notes().observeAll()
    val routines: Flow<List<Routine>> = db.routines().observeAll()
    val entries: Flow<List<RoutineEntry>> = db.entries().observeAll()

    // ---- Notas y recordatorios ----

    suspend fun getNote(id: Long): Note? = db.notes().get(id)

    suspend fun saveNote(note: Note): Long {
        val id = if (note.id == 0L) {
            db.notes().insert(note)
        } else {
            db.notes().update(note)
            note.id
        }
        Notifier.scheduleNote(ctx, note.copy(id = id))
        return id
    }

    suspend fun deleteNote(id: Long) {
        db.notes().delete(id)
        Notifier.cancelNote(ctx, id)
    }

    // ---- Rutinas ----

    fun observeRoutine(id: Long): Flow<Routine?> = db.routines().observe(id)

    fun observeEntries(routineId: Long): Flow<List<RoutineEntry>> = db.entries().observeFor(routineId)

    suspend fun getRoutine(id: Long): Routine? = db.routines().get(id)

    suspend fun saveRoutine(routine: Routine): Long {
        val id = if (routine.id == 0L) {
            db.routines().insert(routine)
        } else {
            db.routines().update(routine)
            routine.id
        }
        Notifier.scheduleRoutine(ctx, routine.copy(id = id))
        return id
    }

    suspend fun deleteRoutine(id: Long) {
        db.routines().delete(id)
        Notifier.cancelRoutine(ctx, id)
    }

    suspend fun getEntry(routineId: Long, day: Long): RoutineEntry? = db.entries().get(routineId, day)

    suspend fun saveEntry(routineId: Long, day: Long, content: String, answersJson: String) {
        val now = System.currentTimeMillis()
        val existing = db.entries().get(routineId, day)
        if (existing == null) {
            db.entries().insert(
                RoutineEntry(
                    routineId = routineId,
                    day = day,
                    content = content,
                    answersJson = answersJson,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } else {
            db.entries().update(existing.copy(content = content, answersJson = answersJson, updatedAt = now))
        }
        if (day == LocalDate.now().toEpochDay()) Notifier.dismissRoutine(ctx, routineId)
    }

    suspend fun deleteEntry(routineId: Long, day: Long) {
        db.entries().delete(routineId, day)
    }

    /** Convierte un texto escrito en «Otro» en opción permanente de la pregunta. */
    suspend fun addOption(routineId: Long, questionId: String, option: String) {
        val text = option.trim()
        if (text.isEmpty()) return
        val routine = db.routines().get(routineId) ?: return
        val questions = questionsFromJson(routine.questionsJson).map { q ->
            if (q.id == questionId && q.options.none { it.equals(text, ignoreCase = true) }) {
                q.copy(options = q.options + text)
            } else {
                q
            }
        }
        db.routines().update(
            routine.copy(questionsJson = questionsToJson(questions), updatedAt = System.currentTimeMillis())
        )
    }

    // ---- Copia de seguridad ----

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "notea")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val notes = JSONArray()
        for (n in db.notes().getAll()) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("title", n.title)
            o.put("content", n.content)
            o.put("isReminder", n.isReminder)
            if (n.reminderAt != null) o.put("reminderAt", n.reminderAt)
            o.put("reminderDone", n.reminderDone)
            if (n.notifiedAt != null) o.put("notifiedAt", n.notifiedAt)
            o.put("pinned", n.pinned)
            o.put("archived", n.archived)
            o.put("tags", n.tags)
            o.put("createdAt", n.createdAt)
            o.put("updatedAt", n.updatedAt)
            notes.put(o)
        }
        root.put("notes", notes)

        val routines = JSONArray()
        for (r in db.routines().getAll()) {
            val o = JSONObject()
            o.put("id", r.id)
            o.put("title", r.title)
            o.put("description", r.description)
            o.put("isForm", r.isForm)
            o.put("template", r.template)
            o.put("questions", JSONArray(r.questionsJson))
            o.put("frequency", r.frequency)
            o.put("daysMask", r.daysMask)
            o.put("dayOfMonth", r.dayOfMonth)
            o.put("timeMinutes", r.timeMinutes)
            o.put("startDay", r.startDay)
            if (r.endDay != null) o.put("endDay", r.endDay)
            o.put("active", r.active)
            o.put("notify", r.notify)
            o.put("createdAt", r.createdAt)
            o.put("updatedAt", r.updatedAt)
            routines.put(o)
        }
        root.put("routines", routines)

        val entries = JSONArray()
        for (e in db.entries().getAll()) {
            val o = JSONObject()
            o.put("id", e.id)
            o.put("routineId", e.routineId)
            o.put("day", e.day)
            o.put("content", e.content)
            o.put("answers", JSONObject(e.answersJson))
            o.put("createdAt", e.createdAt)
            o.put("updatedAt", e.updatedAt)
            entries.put(o)
        }
        root.put("entries", entries)
        return root.toString(2)
    }

    /** Sustituye todos los datos por los de la copia. Devuelve cuántos elementos se han importado. */
    suspend fun importJson(json: String): Int {
        val root = JSONObject(json)
        require(root.optString("app") == "notea") { "El archivo no es una copia de Notea" }

        val notes = ArrayList<Note>()
        val na = root.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until na.length()) {
            val o = na.getJSONObject(i)
            notes.add(
                Note(
                    id = o.optLong("id"),
                    title = o.optString("title"),
                    content = o.optString("content"),
                    isReminder = o.optBoolean("isReminder"),
                    reminderAt = if (o.has("reminderAt")) o.getLong("reminderAt") else null,
                    reminderDone = o.optBoolean("reminderDone"),
                    notifiedAt = if (o.has("notifiedAt")) o.getLong("notifiedAt") else null,
                    pinned = o.optBoolean("pinned"),
                    archived = o.optBoolean("archived"),
                    tags = o.optString("tags"),
                    createdAt = o.optLong("createdAt"),
                    updatedAt = o.optLong("updatedAt"),
                )
            )
        }

        val routines = ArrayList<Routine>()
        val ra = root.optJSONArray("routines") ?: JSONArray()
        for (i in 0 until ra.length()) {
            val o = ra.getJSONObject(i)
            routines.add(
                Routine(
                    id = o.optLong("id"),
                    title = o.optString("title"),
                    description = o.optString("description"),
                    isForm = o.optBoolean("isForm", true),
                    template = o.optString("template"),
                    questionsJson = (o.optJSONArray("questions") ?: JSONArray()).toString(),
                    frequency = o.optString("frequency", "DAILY"),
                    daysMask = o.optInt("daysMask"),
                    dayOfMonth = o.optInt("dayOfMonth", 1),
                    timeMinutes = o.optInt("timeMinutes", 21 * 60),
                    startDay = o.optLong("startDay"),
                    endDay = if (o.has("endDay")) o.getLong("endDay") else null,
                    active = o.optBoolean("active", true),
                    notify = o.optBoolean("notify", true),
                    createdAt = o.optLong("createdAt"),
                    updatedAt = o.optLong("updatedAt"),
                )
            )
        }

        val entries = ArrayList<RoutineEntry>()
        val ea = root.optJSONArray("entries") ?: JSONArray()
        for (i in 0 until ea.length()) {
            val o = ea.getJSONObject(i)
            entries.add(
                RoutineEntry(
                    id = o.optLong("id"),
                    routineId = o.optLong("routineId"),
                    day = o.optLong("day"),
                    content = o.optString("content"),
                    answersJson = (o.optJSONObject("answers") ?: JSONObject()).toString(),
                    createdAt = o.optLong("createdAt"),
                    updatedAt = o.optLong("updatedAt"),
                )
            )
        }

        val routineIds = routines.map { it.id }.toSet()
        db.withTransaction {
            db.entries().clear()
            db.routines().clear()
            db.notes().clear()
            db.notes().insertAll(notes)
            db.routines().insertAll(routines)
            db.entries().insertAll(entries.filter { it.routineId in routineIds })
        }
        Notifier.rescheduleAll(ctx)
        return notes.size + routines.size + entries.size
    }
}
