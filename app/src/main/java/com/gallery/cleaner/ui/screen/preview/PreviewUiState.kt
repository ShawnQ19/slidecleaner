package com.gallery.cleaner.ui.screen.preview

import com.gallery.cleaner.domain.model.MediaItem

data class PreviewUiState(
    val mediaItem: MediaItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
