package com.gallery.cleaner.ui.screen.gallery

import com.gallery.cleaner.domain.model.MediaGroup

data class GalleryUiState(
    val groups: List<MediaGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val error: String? = null
)
