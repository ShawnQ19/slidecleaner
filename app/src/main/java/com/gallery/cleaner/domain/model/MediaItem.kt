package com.gallery.cleaner.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val dateAdded: Long,
    val size: Long,
    val duration: Long? = null,
    val width: Int = 0,
    val height: Int = 0,
    val livePhotoInfo: LivePhotoInfo = LivePhotoInfo(),
    val isQueuedForDelete: Boolean = false
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isLivePhoto: Boolean get() = livePhotoInfo.type != LivePhotoType.NONE && !isVideo
}
