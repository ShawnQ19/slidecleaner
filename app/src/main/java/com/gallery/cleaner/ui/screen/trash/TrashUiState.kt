package com.gallery.cleaner.ui.screen.trash

import com.gallery.cleaner.domain.model.MediaItem

data class TrashUiState(
    val trashItems: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String = "",
    val isSelectMode: Boolean = false,
    val selectedItems: Set<Long> = emptySet()
)
