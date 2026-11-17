package com.notea.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Nota o recordatorio. Un recordatorio es una nota con fecha y hora de aviso. */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val isReminder: Boolean = false,
    val reminderAt: Long? = null,
    val reminderDone: Boolean = false,
    val notifiedAt: Long? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Rutina: diario periódico, en forma de nota libre o de cuestionario. */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val isForm: Boolean = true,
    /** Plantilla Markdown con la que empieza cada entrada (rutinas de tipo nota). */
    val template: String = "",
    /** Preguntas del cuestionario serializadas en JSON. */
    val questionsJson: String = "[]",
    /** DAILY, WEEKLY, MONTHLY o CUSTOM. */
    val frequency: String = "DAILY",
    /** Bits 0..6 = lunes..domingo (para WEEKLY y CUSTOM). */
    val daysMask: Int = 0,
    val dayOfMonth: Int = 1,
    /** Hora del aviso en minutos desde medianoche. */
    val timeMinutes: Int = 21 * 60,
    /** Días desde época (LocalDate.toEpochDay). */
    val startDay: Long = 0,
    val endDay: Long? = null,
    val active: Boolean = true,
    val notify: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Respuesta de una rutina para un día concreto. */
@Entity(
    tableName = "routine_entries",
    indices = [Index(value = ["routineId", "day"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RoutineEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val day: Long,
    val content: String = "",
    val answersJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes")
    suspend fun clear()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun observe(id: Long): Flow<Routine?>

    @Query("SELECT * FROM routines")
    suspend fun getAll(): List<Routine>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun get(id: Long): Routine?

    @Insert
    suspend fun insert(routine: Routine): Long

    @Insert
    suspend fun insertAll(routines: List<Routine>)

    @Update
    suspend fun update(routine: Routine)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM routines")
    suspend fun clear()
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM routine_entries")
    fun observeAll(): Flow<List<RoutineEntry>>

    @Query("SELECT * FROM routine_entries WHERE routineId = :routineId ORDER BY day DESC")
    fun observeFor(routineId: Long): Flow<List<RoutineEntry>>

    @Query("SELECT * FROM routine_entries")
    suspend fun getAll(): List<RoutineEntry>

    @Query("SELECT * FROM routine_entries WHERE routineId = :routineId AND day = :day LIMIT 1")
    suspend fun get(routineId: Long, day: Long): RoutineEntry?

    @Insert
    suspend fun insert(entry: RoutineEntry): Long

    @Insert
    suspend fun insertAll(entries: List<RoutineEntry>)

    @Update
    suspend fun update(entry: RoutineEntry)

    @Query("DELETE FROM routine_entries WHERE routineId = :routineId AND day = :day")
    suspend fun delete(routineId: Long, day: Long)

    @Query("DELETE FROM routine_entries")
    suspend fun clear()
}

@Database(
    entities = [Note::class, Routine::class, RoutineEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDb : RoomDatabase() {
    abstract fun notes(): NoteDao
    abstract fun routines(): RoutineDao
    abstract fun entries(): EntryDao
}
