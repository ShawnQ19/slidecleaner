package com.gallery.cleaner.domain.model

import java.time.YearMonth

data class DeleteQueue(
    val items: List<MediaItem> = emptyList(),
    val currentMonth: YearMonth? = null,
    val monthTotalCount: Int = 0
) {
    companion object {
        const val MAX_QUEUE_SIZE = 50
    }

    val shouldPromptDelete: Boolean
        get() = items.size >= MAX_QUEUE_SIZE || (monthTotalCount > 0 && items.size >= monthTotalCount)
}
