package com.gallery.cleaner.ui.screen.random



import androidx.compose.animation.AnimatedVisibility

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.pager.HorizontalPager

import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.filled.Info

import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material.icons.filled.Shuffle

import androidx.compose.material.icons.filled.CheckCircle

import androidx.compose.material3.Button

import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.SnackbarHost

import androidx.compose.material3.SnackbarHostState

import androidx.compose.material3.Text

import androidx.compose.material3.TopAppBar

import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.hilt.navigation.compose.hiltViewModel

import com.gallery.cleaner.MainActivity

import com.gallery.cleaner.domain.model.DeleteQueue

import com.gallery.cleaner.domain.model.MediaItem

import com.gallery.cleaner.ui.component.AppPadding

import com.gallery.cleaner.ui.component.AppShape

import com.gallery.cleaner.ui.component.GlassCard

import com.gallery.cleaner.ui.component.GlassDialog

import com.gallery.cleaner.ui.component.GlassTopBar

import com.gallery.cleaner.ui.screen.media.DeleteQueueBar

import com.gallery.cleaner.ui.screen.media.SwipeableMediaCard

import com.gallery.cleaner.ui.theme.AppColors

import java.time.Instant

import java.time.LocalDateTime

import java.time.ZoneId

import java.time.format.DateTimeFormatter



@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

@Composable

fun RandomCleanupScreen(

    onBackClick: () -> Unit,

    onMediaClick: (Long) -> Unit,

    viewModel: RandomCleanupViewModel = hiltViewModel()

) {

    val uiState by viewModel.uiState.collectAsState()

    val canUndo by viewModel.undoManager.canUndo.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var topBarHeight by remember { mutableStateOf(0) }

    val topBarHeightDp = with(androidx.compose.ui.platform.LocalDensity.current) { topBarHeight.toDp() }

    val pagerState = rememberPagerState(

        initialPage = uiState.currentIndex,

        pageCount = { uiState.items.size }

    )

    var lastPage by remember { mutableStateOf<Int?>(null) }
    var isSwipingKeptPhoto = false

    LaunchedEffect(uiState.currentIndex, uiState.items.size) {

        if (uiState.items.isNotEmpty() && pagerState.currentPage != uiState.currentIndex) {

            pagerState.scrollToPage(uiState.currentIndex)

        }

    }



    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        val previousPage = lastPage
        if (previousPage != null && currentPage > previousPage) {
            val previousItem = uiState.items.getOrNull(previousPage)
            if (previousItem != null && !isSwipingKeptPhoto) {
                viewModel.keepCurrent()
            }
        }
        isSwipingKeptPhoto = false
        viewModel.setCurrentIndex(currentPage)
        lastPage = currentPage
    }



    LaunchedEffect(uiState.deleteSuccess, uiState.deleteMessage) {

        if (uiState.deleteMessage.isNotBlank()) {

            snackbarHostState.showSnackbar(uiState.deleteMessage)

            viewModel.dismissResultMessage()

        }

    }



    LaunchedEffect(Unit) {

        MainActivity.onTrashResult = { success, count ->

            viewModel.onTrashIntentResult(success, count)

        }

    }



    Box(modifier = Modifier.fillMaxSize()) {

        when {

            uiState.isLoading -> {

                CircularProgressIndicator(

                    modifier = Modifier.align(Alignment.Center),

                    color = AppColors.Primary

                )

            }



            uiState.error != null -> {

                ErrorState(

                    message = uiState.error ?: "加载失败",

                    onRetry = { viewModel.refreshBatch() }

                )

            }



            uiState.currentItem == null -> {

                EmptyRandomState(

                    processedCount = uiState.processedCount,

                    onShuffle = { viewModel.refreshBatch() }

                )

            }



            else -> {

                val browseProgress = if (uiState.items.isNotEmpty()) {

                    (uiState.currentIndex + 1).toFloat() / uiState.items.size.toFloat()

                } else {

                    0f

                }



                Column(modifier = Modifier.fillMaxSize()) {

                    Spacer(modifier = Modifier.height(topBarHeightDp))



                    RandomStatusStrip(

                        currentIndex = uiState.currentIndex + 1,

                        totalCount = uiState.items.size,

                        queuedCount = uiState.deleteQueue.items.size,

                        processedCount = uiState.processedCount,

                        modifier = Modifier.padding(horizontal = AppPadding.LG, vertical = AppPadding.SM)

                    )



                    LinearProgressIndicator(

                        progress = { browseProgress.coerceIn(0f, 1f) },

                        modifier = Modifier.fillMaxWidth(),

                        color = AppColors.Primary,

                        trackColor = AppColors.SurfaceElevated

                    )



                    HorizontalPager(

                        state = pagerState,

                        modifier = Modifier.weight(1f),

                        contentPadding = PaddingValues(0.dp)

                    ) { page ->

                        val item = uiState.items.getOrNull(page)

                        if (item != null) {

                            SwipeableMediaCard(
                                mediaItem = item,
                                onSwipeUp = {
                                    if (viewModel.isItemKept(item)) {
                                        isSwipingKeptPhoto = true
                                        viewModel.unkeepCurrent(item)
                                    }
                                    viewModel.queueForDelete(item)
                                },
                                onTap = { onMediaClick(item.id) },
                                isKept = viewModel.isItemKept(item),
                                modifier = Modifier.fillMaxSize()
                            )

                        }

                    }



                    DeleteQueueBar(
                        deleteQueue = uiState.deleteQueue,
                        onConfirmDelete = { viewModel.showDeleteConfirmDialog() },
                        onUndo = { viewModel.undo() },
                        canUndo = canUndo
                    )

                }

            }

        }



        GlassTopBar(

            modifier = Modifier

                .fillMaxWidth()

                .onGloballyPositioned { topBarHeight = it.size.height }

        ) {

            TopAppBar(

                title = {

                    Column {

                        Text(

                            text = "随机清理",

                            style = MaterialTheme.typography.titleLarge,

                            fontWeight = FontWeight.Black,

                            color = AppColors.TextPrimary

                        )

                        Text(

                            text = "已整理 ${uiState.processedCount} 项 · 随机 ${DeleteQueue.MAX_QUEUE_SIZE} 张",

                            style = MaterialTheme.typography.labelMedium,

                            color = AppColors.TextSecondary

                        )

                    }

                },

                navigationIcon = {

                    IconButton(onClick = { viewModel.onBackPressed(); onBackClick() }) {

                        Icon(

                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,

                            contentDescription = "返回",

                            tint = AppColors.TextPrimary

                        )

                    }

                },

                actions = {

                    if (uiState.deleteQueue.items.isNotEmpty()) {

                        IconButton(onClick = { viewModel.showDeleteConfirmDialog() }) {

                            Icon(

                                imageVector = Icons.Default.Delete,

                                contentDescription = "确认删除",

                                tint = AppColors.Destructive

                            )

                        }

                    }

                    IconButton(onClick = { viewModel.refreshBatch() }) {

                        Icon(

                            imageVector = Icons.Default.Shuffle,

                            contentDescription = "重新随机",

                            tint = AppColors.TextSecondary

                        )

                    }

                },

                colors = TopAppBarDefaults.topAppBarColors(

                    containerColor = Color.Transparent,

                    titleContentColor = AppColors.TextPrimary,

                    navigationIconContentColor = AppColors.TextPrimary,

                    actionIconContentColor = AppColors.TextPrimary

                ),

                modifier = Modifier.statusBarsPadding()

            )

        }



        SnackbarHost(

            hostState = snackbarHostState,

            modifier = Modifier.align(Alignment.BottomCenter)

        )



        if (uiState.showDeleteDialog && uiState.deleteQueue.items.isNotEmpty()) {

            GlassDialog(

                onDismissRequest = { viewModel.dismissDeleteDialog() },

                title = "确认删除",

                text = if (uiState.items.isEmpty()) {
                    "已清理完本组照片，确认删除已加入删除队列的 ${uiState.deleteQueue.items.size} 个项目吗？"
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

                    "确定要将 ${uiState.deleteQueue.items.size} 个项目移入系统回收站吗？可在应用内回收站或系统相册的\"最近删除\"中查看和恢复。"

                } else {

                    "确定要永久删除 ${uiState.deleteQueue.items.size} 个项目吗？当前系统版本不支持回收站功能，删除后无法恢复。"

                },

                confirmText = "删除",

                isDestructive = true,

                onConfirm = { viewModel.confirmDelete() },

                onDismiss = { viewModel.dismissDeleteDialog() }

            )

        }

    }

}



@Composable

private fun RandomStatusStrip(

    currentIndex: Int,

    totalCount: Int,

    queuedCount: Int,

    processedCount: Int,

    modifier: Modifier = Modifier

) {

    GlassCard(

        modifier = modifier.fillMaxWidth(),

        shape = AppShape.XLarge,

        contentPadding = AppPadding.MD

    ) {

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween,

            verticalAlignment = Alignment.CenterVertically

        ) {

            Column(

                modifier = Modifier.weight(1f),

                verticalArrangement = Arrangement.spacedBy(2.dp)

            ) {

                Text(

                    text = "RANDOM BOARD",

                    style = MaterialTheme.typography.labelSmall,

                    color = AppColors.TextTertiary,

                    fontWeight = FontWeight.SemiBold

                )

                Text(

                    text = "第 $currentIndex / $totalCount 张",

                    style = MaterialTheme.typography.titleSmall,

                    color = AppColors.TextPrimary,

                    fontWeight = FontWeight.Black

                )

                Text(

                    text = "左滑自动归类 · 已整理 $processedCount 项",

                    style = MaterialTheme.typography.labelMedium,

                    color = AppColors.TextSecondary

                )

            }



            Row(horizontalArrangement = Arrangement.spacedBy(AppPadding.XS)) {

                RandomStatusChip(

                    label = "QUEUE $queuedCount",

                    tint = if (queuedCount >= DeleteQueue.MAX_QUEUE_SIZE) AppColors.Destructive else AppColors.Primary

                )

                RandomStatusChip(

                    label = "READY",

                    tint = AppColors.Accent

                )

            }

        }

    }

}



@Composable

private fun RandomStatusChip(

    label: String,

    tint: Color

) {

    Box(

        modifier = Modifier

            .clip(AppShape.Pill)

            .background(tint.copy(alpha = 0.14f))

            .border(1.dp, tint.copy(alpha = 0.24f), AppShape.Pill)

            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)

    ) {

        Text(

            text = label,

            style = MaterialTheme.typography.labelSmall,

            color = tint,

            fontWeight = FontWeight.SemiBold

        )

    }

}



@Composable

private fun RandomSummaryCard(

    processedCount: Int,

    remainingCount: Int,

    modifier: Modifier = Modifier

) {

    GlassCard(

        modifier = modifier.fillMaxWidth(),

        shape = AppShape.XLarge,

        contentPadding = AppPadding.LG

    ) {

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.spacedBy(AppPadding.SM)

        ) {

            SummaryTile(label = "已整理", value = processedCount.toString(), tint = AppColors.Primary, modifier = Modifier.weight(1f))

            SummaryTile(label = "本轮剩余", value = remainingCount.toString(), tint = AppColors.Accent, modifier = Modifier.weight(1f))

            SummaryTile(label = "批次", value = DeleteQueue.MAX_QUEUE_SIZE.toString(), tint = AppColors.TextSecondary, modifier = Modifier.weight(1f))

        }

    }

}



@Composable

private fun SummaryTile(

    label: String,

    value: String,

    tint: Color,

    modifier: Modifier = Modifier

) {

    Box(

        modifier = modifier

            .clip(AppShape.Medium)

            .background(AppColors.SurfaceElevated.copy(alpha = 0.8f))

            .border(1.dp, AppColors.Separator, AppShape.Medium)

            .padding(horizontal = AppPadding.MD, vertical = AppPadding.SM)

    ) {

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

            Text(

                text = value,

                style = MaterialTheme.typography.titleMedium,

                color = AppColors.TextPrimary,

                fontWeight = FontWeight.Black

            )

            Text(

                text = label,

                style = MaterialTheme.typography.labelSmall,

                color = tint,

                fontWeight = FontWeight.SemiBold

            )

        }

    }

}



@Composable

private fun RandomMediaBadge(

    label: String,

    tint: Color,

    modifier: Modifier = Modifier

) {

    Box(

        modifier = modifier

            .clip(AppShape.Pill)

            .background(tint.copy(alpha = 0.14f))

            .border(1.dp, tint.copy(alpha = 0.24f), AppShape.Pill)

            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)

    ) {

        Text(

            text = label,

            style = MaterialTheme.typography.labelSmall,

            color = tint,

            fontWeight = FontWeight.SemiBold

        )

    }

}



@Composable

private fun RandomInfoBar(item: MediaItem) {

    GlassCard(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = AppPadding.LG, vertical = AppPadding.SM),

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

                        text = if (item.isVideo) "随机视频" else "随机照片",

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

                        text = formatFileSize(item.size),

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

                InfoChip(label = formatDate(item.dateAdded))

                InfoChip(label = if (item.isVideo) "视频" else "照片")

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

private fun EmptyRandomState(

    processedCount: Int,

    onShuffle: () -> Unit

) {

    Box(

        modifier = Modifier

            .fillMaxSize()

            .padding(horizontal = AppPadding.XXL),

        contentAlignment = Alignment.Center

    ) {

        GlassCard(

            modifier = Modifier.fillMaxWidth(),

            shape = AppShape.XLarge,

            contentPadding = AppPadding.XXL

        ) {

            Column(verticalArrangement = Arrangement.spacedBy(AppPadding.MD)) {

                Text(

                    text = "RANDOM QUEUE COMPLETE",

                    style = MaterialTheme.typography.labelSmall,

                    color = AppColors.TextTertiary,

                    fontWeight = FontWeight.SemiBold

                )

                Text(

                    text = "当前批次已处理完毕",

                    style = MaterialTheme.typography.headlineSmall,

                    color = AppColors.TextPrimary,

                    fontWeight = FontWeight.Black

                )

                Text(

                    text = "已整理 $processedCount 项。可以继续随机抽下一批 50 张。",

                    style = MaterialTheme.typography.bodyMedium,

                    color = AppColors.TextSecondary

                )

                Button(

                    onClick = onShuffle,

                    modifier = Modifier.fillMaxWidth(),

                    shape = AppShape.Medium,

                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),

                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)

                ) {

                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))

                    Spacer(modifier = Modifier.size(6.dp))

                    Text("再随机 50 张", fontWeight = FontWeight.SemiBold)

                }

            }

        }

    }

}



@Composable

private fun ErrorState(

    message: String,

    onRetry: () -> Unit

) {

    Box(

        modifier = Modifier

            .fillMaxSize()

            .padding(horizontal = AppPadding.XXL),

        contentAlignment = Alignment.Center

    ) {

        GlassCard(

            modifier = Modifier.fillMaxWidth(),

            shape = AppShape.XLarge,

            contentPadding = AppPadding.XXL

        ) {

            Column(verticalArrangement = Arrangement.spacedBy(AppPadding.SM)) {

                Text(

                    text = "加载失败",

                    style = MaterialTheme.typography.titleMedium,

                    color = AppColors.Destructive,

                    fontWeight = FontWeight.SemiBold

                )

                Text(

                    text = message,

                    style = MaterialTheme.typography.bodyMedium,

                    color = AppColors.TextSecondary

                )

                Button(onClick = onRetry) { Text("重试") }

            }

        }

    }

}



private fun formatDate(dateAddedSeconds: Long): String {

    val dateTime = LocalDateTime.ofInstant(

        Instant.ofEpochSecond(dateAddedSeconds),

        ZoneId.systemDefault()

    )

    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    return dateTime.format(formatter)

}



private fun formatFileSize(size: Long): String {

    return when {

        size < 1024 -> "$size B"

        size < 1024 * 1024 -> "${size / 1024} KB"

        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))

        else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))

    }

}