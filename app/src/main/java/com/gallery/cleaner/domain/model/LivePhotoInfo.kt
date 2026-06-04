package com.gallery.cleaner.domain.model

import android.net.Uri

data class LivePhotoInfo(
    val type: LivePhotoType = LivePhotoType.NONE,
    val videoOffset: Long = 0,
    val videoLength: Long = 0,
    val pairedVideoUri: Uri? = null
)
