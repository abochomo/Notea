package com.notea.data.mapper

import com.notea.data.local.entity.NoteEntity
import com.notea.domain.model.Note
import java.time.Instant

fun NoteEntity.toDomain(): Note = Note(
    noteId = noteId,
    title = title,
    content = content,
    isFavorite = isFavorite,
    isArchived = isArchived,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)
