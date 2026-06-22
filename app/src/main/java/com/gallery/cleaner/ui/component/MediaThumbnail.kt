package com.gallery.cleaner.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
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

    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaItem.uri)
                .size(size)
                .crossfade(true)
                .build(),
            contentDescription = mediaItem.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.SurfaceOverlay)
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.SurfaceOverlay)
                )
            }
        )

        if (mediaItem.isVideo) {
            VideoIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
