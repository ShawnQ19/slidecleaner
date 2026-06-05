package com.gallery.cleaner.ui.screen.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.component.Motion
import com.gallery.cleaner.ui.component.pressClick
import com.gallery.cleaner.ui.theme.AppColors
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_PREVIEW_SCALE = 1f
private const val MAX_PREVIEW_SCALE = 5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    mediaId: Long,
    onBackClick: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInfo by remember { mutableStateOf(true) }
    val previewSnackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppColors.Primary,
                    strokeWidth = 3.dp
                )
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppPadding.XXL),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "加载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.Destructive,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(AppPadding.SM))
                    Text(
                        text = uiState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }
            uiState.mediaItem != null -> {
                val item = uiState.mediaItem!!

                Column(modifier = Modifier.fillMaxSize()) {
                    GlassTopBar(modifier = Modifier.fillMaxWidth()) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "预览",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary
                                    )
                                    Text(
                                        text = previewHint(item),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.TextSecondary
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                        tint = AppColors.TextPrimary
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { showInfo = !showInfo }) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "信息",
                                        tint = if (showInfo) AppColors.Primary else AppColors.TextSecondary
                                    )
                                }
                                val context = LocalContext.current
                                val scope = rememberCoroutineScope()
                                IconButton(onClick = {
                                    scope.launch {
                                        exportLogs(context, previewSnackbarHostState)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "导出日志",
                                        tint = AppColors.TextSecondary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                navigationIconContentColor = AppColors.TextPrimary,
                                actionIconContentColor = AppColors.TextPrimary
                            ),
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = AppPadding.LG, vertical = AppPadding.MD)
                            .clip(AppShape.XLarge)
                            .background(AppColors.Surface)
                            .border(1.dp, AppColors.Separator, AppShape.XLarge)
                    ) {
                        when {
                            item.isVideo -> {
                                VideoPlayer(
                                    uri = item.uri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            item.isLivePhoto -> {
                                MotionPhotoPlayer(
                                    mediaItem = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                val context = LocalContext.current
                                ZoomablePreviewImage(
                                    zoomKey = item.id,
                                    imageModel = ImageRequest.Builder(context)
                                        .data(item.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(AppPadding.LG)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showInfo,
                        enter = Motion.Enter.slideInFromBottom(),
                        exit = Motion.Exit.slideOutToBottom(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppPadding.LG, vertical = AppPadding.MD)
                    ) {
                        PreviewInfoBar(item = item)
                    }
                }
            }
        }
        SnackbarHost(
            hostState = previewSnackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PreviewInfoBar(item: MediaItem) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.XLarge,
        contentPadding = AppPadding.LG
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppPadding.SM)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = AppColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = previewHint(item),
                        color = AppColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(AppShape.Pill)
                        .background(AppColors.Primary.copy(alpha = 0.12f))
                        .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)
                ) {
                    Text(
                        text = previewTag(item),
                        color = AppColors.Primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppPadding.SM)
            ) {
                val dateTime = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(item.dateAdded),
                    java.time.ZoneId.systemDefault()
                )
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

                InfoChip(label = formatFileSize(item.size))
                InfoChip(label = dateTime.format(formatter))
                if (item.isVideo) {
                    InfoChip(label = "视频")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String) {
    Box(
        modifier = Modifier
            .clip(AppShape.Small)
            .background(AppColors.SurfaceOverlay.copy(alpha = 0.6f))
            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)
    ) {
        Text(
            text = label,
            color = AppColors.TextTertiary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ZoomablePreviewImage(
    zoomKey: Any,
    imageModel: Any,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var scale by remember(zoomKey) { mutableFloatStateOf(MIN_PREVIEW_SCALE) }
    var offset by remember(zoomKey) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(MIN_PREVIEW_SCALE, MAX_PREVIEW_SCALE)
        scale = newScale
        if (newScale <= MIN_PREVIEW_SCALE) {
            offset = Offset.Zero
        } else {
            offset += Offset(
                x = panChange.x * newScale,
                y = panChange.y * newScale
            )
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
            .pointerInput(zoomKey) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > MIN_PREVIEW_SCALE) {
                            scale = MIN_PREVIEW_SCALE
                            offset = Offset.Zero
                        } else {
                            scale = 2f
                        }
                    }
                )
            }
            .transformable(state = transformState)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun previewHint(item: MediaItem): String {
    return when {
        item.isVideo -> "长按可 2x 加速"
        item.isLivePhoto -> "长按播放嵌入视频 / 双指缩放"
        else -> "双击放大 / 双指缩放 / 拖动查看"
    }
}

private fun previewTag(item: MediaItem): String {
    return when {
        item.isVideo -> "VIDEO"
        item.isLivePhoto -> "LIVE"
        else -> "PHOTO"
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}


private suspend fun exportLogs(context: android.content.Context, snackbarHostState: SnackbarHostState) {
    try {
            com.gallery.cleaner.util.log.AppLogger.flush()
            val logFiles = com.gallery.cleaner.util.log.AppLogger.getLogFiles()
            if (logFiles.isEmpty()) {
                withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("\u6682\u65e0\u65e5\u5fd7") }
                return
            }
            val exportDir = java.io.File(context.getExternalFilesDir(null) ?: context.filesDir, "log_export")
            if (!exportDir.exists()) exportDir.mkdirs()
            val logFile = logFiles.first()
            val exportFile = java.io.File(exportDir, logFile.name)
            logFile.copyTo(exportFile, overwrite = true)
            withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("\u65e5\u5fd7\u5df2\u5bfc\u51fa: ${exportFile.absolutePath}") }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("\u5bfc\u51fa\u5931\u8d25: ${e.message}") }
        }
    }