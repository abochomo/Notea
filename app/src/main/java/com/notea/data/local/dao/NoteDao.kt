package com.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notea.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeNonArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchNonArchivedNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE noteId = :noteId")
    suspend fun getById(noteId: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE noteId = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("SELECT * FROM notes WHERE createdAt BETWEEN :from AND :to OR updatedAt BETWEEN :from AND :to")
    suspend fun getActivityBetween(from: Long, to: Long): List<NoteEntity>
}
