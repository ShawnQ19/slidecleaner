package com.gallery.cleaner.ui.screen.media

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassSwipeHint
import com.gallery.cleaner.ui.component.VideoIndicator
import com.gallery.cleaner.ui.theme.AppColors
import kotlin.math.absoluteValue

private const val SWIPE_THRESHOLD = 120f
private const val SWIPE_MAX_OFFSET = 300f

@Composable
fun SwipeableMediaCard(
    mediaItem: MediaItem,
    onSwipeUp: () -> Unit,
    onTap: () -> Unit,
    swipeResetKey: Int = 0,
    isKept: Boolean = false,
    modifier: Modifier = Modifier
) {
    var offsetY by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(0f) }
    var overlayAlpha by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(0f) }
    var targetScale by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(1f) }
    var photoScale by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(1f) }
    var photoOffsetX by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(0f) }
    var photoOffsetY by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(0f) }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "swipe_card_scale"
    )
    val isZoomablePhoto = !mediaItem.isVideo && !mediaItem.isLivePhoto
    val isPhotoZoomed = isZoomablePhoto && photoScale > 1.01f

    LaunchedEffect(mediaItem.id, swipeResetKey) {
        offsetY = 0f
        overlayAlpha = 0f
        targetScale = 1f
        photoScale = 1f
        photoOffsetX = 0f
        photoOffsetY = 0f
    }

    fun resetSwipeState() {
        offsetY = 0f
        overlayAlpha = 0f
        targetScale = 1f
    }

    val hintProgress = if (offsetY < 0f) {
        (0.22f + overlayAlpha * 0.78f).coerceIn(0.22f, 1f)
    } else {
        0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        GlassSwipeHint(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = AppPadding.XXL, vertical = AppPadding.SM)
                .zIndex(2f)
                .fillMaxWidth(),
            progress = hintProgress
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .graphicsLayer {
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(mediaItem.id, isPhotoZoomed) {
                    if (!isPhotoZoomed) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                targetScale = 0.98f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val rawOffset = offsetY + dragAmount
                                val newOffset = rawOffset.coerceIn(-SWIPE_MAX_OFFSET, 50f)
                                val progress = if (newOffset < 0f) {
                                    (-newOffset / SWIPE_MAX_OFFSET).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                offsetY = newOffset
                                overlayAlpha = progress
                            },
                            onDragEnd = {
                                if (offsetY < -SWIPE_THRESHOLD) {
                                    targetScale = 0.9f
                                    onSwipeUp()
                                } else {
                                    resetSwipeState()
                                }
                            },
                            onDragCancel = {
                                resetSwipeState()
                            }
                        )
                    }
                }
                .pointerInput(mediaItem.id) {
                    detectTapGestures(
                        onTap = {
                            if (!isPhotoZoomed) {
                                onTap()
                            }
                        }
                    )
                }
        ) {
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(AppShape.XLarge)
                    .border(1.dp, AppColors.Separator, AppShape.XLarge)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(mediaItem.uri).crossfade(false).build(),
                    contentDescription = mediaItem.name,
                    contentScale = if (mediaItem.isVideo) ContentScale.Crop else ContentScale.Fit,
                    modifier = if (isZoomablePhoto) {
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = photoScale
                                scaleY = photoScale
                                translationX = photoOffsetX
                                translationY = photoOffsetY
                            }
                            .pointerInput(mediaItem.id) {
                                awaitEachGesture {
                                    var zooming = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressedPointers = event.changes.count { it.pressed }

                                        if (pressedPointers >= 2) {
                                            zooming = true
                                            val newScale = (photoScale * event.calculateZoom()).coerceIn(1f, 5f)
                                            photoScale = newScale
                                            if (newScale <= 1f) {
                                                photoOffsetX = 0f
                                                photoOffsetY = 0f
                                            } else {
                                                val panChange = event.calculatePan()
                                                photoOffsetX += panChange.x * newScale
                                                photoOffsetY += panChange.y * newScale
                                            }
                                            event.changes.forEach { it.consume() }
                                        } else if (zooming && pressedPointers == 0) {
                                            break
                                        } else if (pressedPointers == 0) {
                                            break
                                        }
                                    }
                                }
                            }
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
                if (mediaItem.isVideo) {
                    VideoIndicator(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 16.dp)
                .zIndex(1f)
                .clip(AppShape.Pill)
                .background(AppColors.Surface.copy(alpha = 0.78f))
                .border(1.dp, AppColors.Separator, AppShape.Pill)
                .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)
        ) {
            Text(
                text = when {
                    mediaItem.isVideo -> "VIDEO"
                    mediaItem.isLivePhoto -> "LIVE"
                    else -> "PHOTO"
                },
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (isKept) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp)
                    .zIndex(1f)
                    .clip(AppShape.Pill)
                    .background(AppColors.Success)
                    .padding(horizontal = AppPadding.XS, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已保留",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
