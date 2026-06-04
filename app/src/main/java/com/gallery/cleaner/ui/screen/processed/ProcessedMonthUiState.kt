package com.gallery.cleaner.ui.screen.processed

import com.gallery.cleaner.domain.model.MediaItem

data class ProcessedMonthUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)