package com.gallery.cleaner.ui.screen.media

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val SWIPE_THRESHOLD = 100f
private const val SWIPE_MAX_OFFSET = 180f
private const val HINT_MIN_OFFSET = 30f
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f

@Composable
fun SwipeableMediaCard(
    mediaItem: MediaItem,
    onSwipeUp: () -> Unit,
    onTap: () -> Unit,
    swipeResetKey: Int = 0,
    isKept: Boolean = false,
    modifier: Modifier = Modifier
) {
    val offsetY = remember(mediaItem.id, swipeResetKey) { Animatable(0f) }
    val overlayAlpha = remember(mediaItem.id, swipeResetKey) { Animatable(0f) }
    var targetScale by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(1f) }
    var photoScale by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(1f) }
    var photoOffsetX by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(0f) }
    var photoOffsetY by remember(mediaItem.id, swipeResetKey) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "swipe_card_scale"
    )

    val isZoomablePhoto = !mediaItem.isVideo && !mediaItem.isLivePhoto
    val isPhotoZoomed = isZoomablePhoto && photoScale > 1.01f

    LaunchedEffect(mediaItem.id, swipeResetKey) {
        offsetY.snapTo(0f)
        overlayAlpha.snapTo(0f)
        targetScale = 1f
        photoScale = 1f
        photoOffsetX = 0f
        photoOffsetY = 0f
    }

    val hintProgress = if (offsetY.value < -HINT_MIN_OFFSET) {
        val adjustedAlpha = ((-offsetY.value - HINT_MIN_OFFSET) / (SWIPE_MAX_OFFSET - HINT_MIN_OFFSET)).coerceIn(0f, 1f)
        (0.22f + adjustedAlpha * 0.78f).coerceIn(0.22f, 1f)
    } else 0f

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        GlassSwipeHint(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = AppPadding.XXL, vertical = AppPadding.SM)
                .zIndex(2f),
            progress = hintProgress
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .graphicsLayer {
                    translationY = offsetY.value
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
                                scope.launch {
                                    val rawOffset = offsetY.value + dragAmount
                                    val newOffset = rawOffset.coerceIn(-SWIPE_MAX_OFFSET, 50f)
                                    val progress = if (newOffset < 0f) {
                                        (-newOffset / SWIPE_MAX_OFFSET).coerceIn(0f, 1f)
                                    } else 0f
                                    offsetY.snapTo(newOffset)
                                    overlayAlpha.snapTo(progress)
                                }
                            },
                            onDragEnd = {
                                if (offsetY.value < -SWIPE_THRESHOLD) {
                                    targetScale = 0.9f
                                    onSwipeUp()
                                } else {
                                    scope.launch {
                                        launch {
                                            offsetY.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                        launch {
                                            overlayAlpha.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    }
                                    targetScale = 1f
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    launch {
                                        offsetY.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    launch {
                                        overlayAlpha.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                                targetScale = 1f
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
                    model = ImageRequest.Builder(context)
                        .data(mediaItem.uri)
                        .crossfade(false)
                        .build(),
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
                                            val newScale =
                                                (photoScale * event.calculateZoom()).coerceIn(
                                                    MIN_ZOOM,
                                                    MAX_ZOOM
                                                )
                                            photoScale = newScale
                                            if (newScale <= MIN_ZOOM) {
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
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
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
