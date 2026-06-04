package com.gallery.cleaner.ui.screen.random

import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.model.MediaItem

data class RandomCleanupUiState(
    val batchItems: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val deleteQueue: DeleteQueue = DeleteQueue(),
    val processedCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteMessage: String = ""
) {
    val currentItem: MediaItem? get() = batchItems.getOrNull(currentIndex)
    val remainingInBatch: Int get() = batchItems.size
    val isBusy: Boolean get() = showDeleteDialog
}