package com.gallery.cleaner.ui.screen.preview

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.cleaner.domain.model.LivePhotoType
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.util.LivePhotoExtractor
import com.gallery.cleaner.util.log.AppLogger
import androidx.media3.common.PlaybackException
import java.io.File

private const val TAG = "MotionPhotoPlayer"
private const val LIVE_PHOTO_MIN_SCALE = 1f
private const val LIVE_PHOTO_MAX_SCALE = 5f

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MotionPhotoPlayer(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoUri by remember(mediaItem.id) { mutableStateOf<Uri?>(null) }
    var isPlaying by remember(mediaItem.id) { mutableStateOf(false) }
    var scale by remember(mediaItem.id) { mutableFloatStateOf(LIVE_PHOTO_MIN_SCALE) }
    var offset by remember(mediaItem.id) { mutableStateOf(Offset.Zero) }

    AppLogger.i(TAG, "Composing MotionPhotoPlayer: id=${mediaItem.id}, name=${mediaItem.name}, " +
        "liveType=${mediaItem.livePhotoInfo.type}, isVideo=${mediaItem.isVideo}, " +
        "videoOffset=${mediaItem.livePhotoInfo.videoOffset}, videoLength=${mediaItem.livePhotoInfo.videoLength}, " +
        "pairedVideoUri=${mediaItem.livePhotoInfo.pairedVideoUri}")

    androidx.compose.runtime.LaunchedEffect(mediaItem) {
        AppLogger.i(TAG, "LaunchedEffect: 开始准备视频 URI, mediaId=${mediaItem.id}")
        videoUri = prepareVideoUri(context, mediaItem)
        AppLogger.i(TAG, "LaunchedEffect: 视频URI准备完成, videoUri=$videoUri")
    }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(LIVE_PHOTO_MIN_SCALE, LIVE_PHOTO_MAX_SCALE)
        scale = newScale
        if (newScale <= LIVE_PHOTO_MIN_SCALE) {
            offset = Offset.Zero
        } else {
            offset += Offset(
                x = panChange.x * newScale,
                y = panChange.y * newScale
            )
        }
    }

    // 当 videoUri 变化时创建/重建 ExoPlayer
    DisposableEffect(videoUri) {
        val currentUri = videoUri
        if (currentUri == null) {
            exoPlayer = null
            onDispose { }
        } else {
            AppLogger.i(TAG, "创建 ExoPlayer: videoUri=$currentUri")
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(ExoMediaItem.fromUri(currentUri))
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        AppLogger.i(TAG, "ExoPlayer state: $playbackState")
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        AppLogger.e(TAG, "ExoPlayer error: ${error.message}, code=${error.errorCode}")
                    }
                })
                prepare()
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                playWhenReady = false
            }
            exoPlayer = player
            onDispose {
                AppLogger.i(TAG, "释放 ExoPlayer")
                player.release()
                exoPlayer = null
                if (currentUri is android.net.Uri && currentUri.scheme == "file") {
                    try { java.io.File(currentUri.path!!).delete() } catch (_: Exception) {}
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(isPlaying) {
        AppLogger.i(TAG, "播放状态变化: isPlaying=$isPlaying, exoPlayer=${exoPlayer != null}")
        exoPlayer?.playWhenReady = isPlaying
        if (!isPlaying) {
            exoPlayer?.pause()
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(mediaItem.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!down.pressed) return@awaitEachGesture
                    AppLogger.d(TAG, "手指按下: videoUri=${videoUri != null}, exoPlayer=${exoPlayer != null}")
                    // 等待长按判定（400ms）
                    val longPress = withTimeoutOrNull(400L) {
                        var held = true
                        while (held) {
                            val event = awaitPointerEvent()
                            held = event.changes.any { it.pressed }
                        }
                        true // 手指在超时前松开了 → 短按
                    }
                    if (longPress == null) {
                        // 超时 → 长按开始播放
                        AppLogger.i(TAG, "长按触发: videoUri=${videoUri != null}, exoPlayer=${exoPlayer != null}")
                        if (videoUri != null && exoPlayer != null) {
                            isPlaying = true
                            AppLogger.i(TAG, "开始播放 Live Photo 视频")
                        } else {
                            AppLogger.w(TAG, "无法播放: videoUri=${videoUri}, exoPlayer=${exoPlayer != null}")
                        }
                        // 等待手指松开
                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (!anyPressed) {
                                isPlaying = false
                                AppLogger.i(TAG, "手指松开，停止播放")
                                break
                            }
                        }
                    }
                }
            }
            .transformable(state = transformState)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaItem.uri)
                .crossfade(true)
                .build(),
            contentDescription = mediaItem.name,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        if (isPlaying && videoUri != null && exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    AppLogger.i(TAG, "创建 PlayerView")
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        controllerAutoShow = false
                        controllerShowTimeoutMs = 0
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        LivePhotoBadge(
            type = mediaItem.livePhotoInfo.type,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
fun LivePhotoBadge(
    type: LivePhotoType,
    modifier: Modifier = Modifier
) {
    val label = when (type) {
        LivePhotoType.GOOGLE_PIXEL -> "Motion"
        LivePhotoType.XIAOMI -> "MicroVideo"
        LivePhotoType.OPPO -> "O-Live"
        LivePhotoType.VIVO -> "动态照片"
        LivePhotoType.HUAWEI -> "动态照片"
        LivePhotoType.APPLE -> "Live"
        else -> "Live"
    }

    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Live Photo",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

private fun prepareVideoUri(context: android.content.Context, mediaItem: MediaItem): Uri? {
    AppLogger.enter(TAG, "prepareVideoUri", "id" to mediaItem.id, "type" to mediaItem.livePhotoInfo.type,
        "videoOffset" to mediaItem.livePhotoInfo.videoOffset, "videoLength" to mediaItem.livePhotoInfo.videoLength,
        "pairedVideoUri" to mediaItem.livePhotoInfo.pairedVideoUri)

    val result = when (mediaItem.livePhotoInfo.type) {
        LivePhotoType.VIVO, LivePhotoType.APPLE -> {
            val tempFile = File(context.cacheDir, "live_photo_${mediaItem.id}.mp4")
            AppLogger.i(TAG, "VIVO/Apple: 提取配对视频到临时文件: ${tempFile.absolutePath}")
            val extracted = LivePhotoExtractor.extractVideo(context, mediaItem, tempFile)
            if (extracted) {
                AppLogger.i(TAG, "VIVO/Apple: 视频提取成功, 文件大小: ${tempFile.length()} bytes")
                Uri.fromFile(tempFile)
            } else {
                AppLogger.w(TAG, "VIVO/Apple: 视频提取失败")
                null
            }
        }
        LivePhotoType.GOOGLE_PIXEL, LivePhotoType.XIAOMI, LivePhotoType.OPPO, LivePhotoType.HUAWEI -> {
            val tempFile = File(context.cacheDir, "live_photo_${mediaItem.id}.mp4")
            AppLogger.i(TAG, "尝试提取嵌入视频到: ${tempFile.absolutePath}")
            val extracted = LivePhotoExtractor.extractVideo(context, mediaItem, tempFile)
            if (extracted) {
                AppLogger.i(TAG, "嵌入视频提取成功, 文件大小: ${tempFile.length()} bytes")
                Uri.fromFile(tempFile)
            } else {
                AppLogger.w(TAG, "嵌入视频提取失败")
                null
            }
        }
        else -> {
            AppLogger.w(TAG, "未知的 Live Photo 类型: ${mediaItem.livePhotoInfo.type}")
            null
        }
    }

    AppLogger.exit(TAG, "prepareVideoUri", "result=$result")
    return result
}
