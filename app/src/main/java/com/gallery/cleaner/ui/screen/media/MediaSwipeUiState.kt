package com.gallery.cleaner.ui.screen.media

import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.model.MediaItem

data class CleanupUiState(
    val items: List<MediaItem> = emptyList(),
    val hiddenItemIds: Set<Long> = emptySet(),
    val currentIndex: Int = 0,
    val deleteQueue: DeleteQueue = DeleteQueue(),
    val processedCount: Int = 0,
    val batchTotal: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteMessage: String = ""
) {
    val currentItem: MediaItem? get() = visibleItems.getOrNull(currentIndex)
    val visibleItems: List<MediaItem> get() = items.filter { it.id !in hiddenItemIds }
    val remainingInBatch: Int get() = items.size
    val isBusy: Boolean get() = showDeleteDialog
    val isBatchComplete: Boolean get() = !isLoading && items.isEmpty() && !showDeleteDialog
    val keptCount: Int get() = batchTotal - deleteQueue.items.size - items.size
}

typealias MediaSwipeUiState = CleanupUiState
