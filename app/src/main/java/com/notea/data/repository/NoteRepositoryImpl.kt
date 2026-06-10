package com.notea.data.repository

import com.notea.data.local.dao.NoteDao
import com.notea.data.local.entity.NoteEntity
import com.notea.data.mapper.toDomain
import com.notea.domain.model.Note
import com.notea.domain.model.NoteDraft
import com.notea.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val clock: Clock
) : NoteRepository {

    override fun observeActiveNotes(query: String): Flow<List<Note>> {
        val source = if (query.isBlank()) noteDao.observeNonArchivedNotes() else noteDao.searchNonArchivedNotes(query)
        return source.map { items -> items.map { it.toDomain() } }
    }

    override fun observeFavoriteNotes(): Flow<List<Note>> =
        noteDao.observeFavoriteNotes().map { items -> items.map { it.toDomain() } }

    override fun observeArchivedNotes(): Flow<List<Note>> =
        noteDao.observeArchivedNotes().map { items -> items.map { it.toDomain() } }

    override suspend fun getNoteById(noteId: Long): Note? = noteDao.getById(noteId)?.toDomain()

    override suspend fun upsert(draft: NoteDraft): Long {
        val now = clock.millis()
        val existing = draft.noteId?.let { noteDao.getById(it) }
        val entity = if (existing == null) {
            NoteEntity(
                title = draft.title.trim(),
                content = draft.content.trim(),
                isFavorite = draft.isFavorite,
                isArchived = draft.isArchived,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing.copy(
                title = draft.title.trim(),
                content = draft.content.trim(),
                isFavorite = draft.isFavorite,
                isArchived = draft.isArchived,
                updatedAt = now
            )
        }
        return noteDao.insert(entity)
    }

    override suspend fun delete(noteId: Long) {
        noteDao.deleteById(noteId)
    }

    override suspend fun toggleFavorite(noteId: Long) {
        val current = noteDao.getById(noteId) ?: return
        noteDao.update(current.copy(isFavorite = !current.isFavorite, updatedAt = clock.millis()))
    }

    override suspend fun toggleArchive(noteId: Long) {
        val current = noteDao.getById(noteId) ?: return
        noteDao.update(current.copy(isArchived = !current.isArchived, updatedAt = clock.millis()))
    }
}
