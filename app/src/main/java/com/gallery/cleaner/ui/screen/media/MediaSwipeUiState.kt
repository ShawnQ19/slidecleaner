package com.gallery.cleaner.ui.screen.media

import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.model.MediaItem

data class CleanupUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val deleteQueue: DeleteQueue = DeleteQueue(),
    val processedCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteMessage: String = ""
) {
    val currentItem: MediaItem? get() = items.getOrNull(currentIndex)
    val remainingInBatch: Int get() = items.size
    val isBusy: Boolean get() = showDeleteDialog
}

typealias MediaSwipeUiState = CleanupUiState