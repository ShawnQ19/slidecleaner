package com.gallery.cleaner.domain.model

import java.time.YearMonth

data class MediaGroup(
    val yearMonth: YearMonth,
    val label: String,
    val mediaItems: List<MediaItem>,
    val thumbnail: MediaItem? = null
)
