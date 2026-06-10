package com.notea.domain.repository

import com.notea.domain.model.Note
import com.notea.domain.model.NoteDraft
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeActiveNotes(query: String = ""): Flow<List<Note>>
    fun observeFavoriteNotes(): Flow<List<Note>>
    fun observeArchivedNotes(): Flow<List<Note>>
    suspend fun getNoteById(noteId: Long): Note?
    suspend fun upsert(draft: NoteDraft): Long
    suspend fun delete(noteId: Long)
    suspend fun toggleFavorite(noteId: Long)
    suspend fun toggleArchive(noteId: Long)
}
