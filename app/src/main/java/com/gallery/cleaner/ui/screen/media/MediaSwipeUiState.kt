package com.gallery.cleaner.ui.screen.media

import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.model.MediaItem

data class MediaSwipeUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val deleteQueue: DeleteQueue = DeleteQueue(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteMessage: String = ""
)
