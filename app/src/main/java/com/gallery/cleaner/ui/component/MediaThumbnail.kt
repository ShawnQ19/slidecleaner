package com.gallery.cleaner.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.ui.theme.AppColors

@Composable
fun MediaThumbnail(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    size: Int = 480
) {
    val context = LocalContext.current
    val request = remember(mediaItem.uri, size) {
        ImageRequest.Builder(context)
            .data(mediaItem.uri)
            .size(size)
            .build()
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = request,
            contentDescription = mediaItem.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = null,
            error = null,
            fallback = null
        )

        if (mediaItem.isVideo) {
            VideoIndicator(
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
            )
        }
    }
}
