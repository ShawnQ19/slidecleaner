package com.gallery.cleaner.ui.navigation

object Routes {
    const val GALLERY = "gallery"
    const val PROCESSED_GALLERY = "processed_gallery"
    const val MEDIA_SWIPE = "media_swipe/{yearMonth}"
    const val PROCESSED_MONTH = "processed_month/{yearMonth}"
    const val PREVIEW = "preview/{mediaId}"
    const val RANDOM_CLEANUP = "random_cleanup"
    const val TRASH = "trash"
    const val THEME_SETTINGS = "theme_settings"

    fun mediaSwipe(yearMonth: String) = "media_swipe/$yearMonth"
    fun processedGallery() = PROCESSED_GALLERY
    fun processedMonth(yearMonth: String) = "processed_month/$yearMonth"
    fun preview(mediaId: Long) = "preview/$mediaId"
    fun randomCleanup() = RANDOM_CLEANUP
}
