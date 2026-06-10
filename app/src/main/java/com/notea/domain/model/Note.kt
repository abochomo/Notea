package com.notea.domain.model

import java.time.Instant

data class Note(
    val noteId: Long,
    val title: String,
    val content: String,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class NoteDraft(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false
)

data class RecentItem(
    val id: Long,
    val type: RecentItemType,
    val title: String,
    val subtitle: String,
    val timestamp: Instant
)
