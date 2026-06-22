package com.gallery.cleaner.ui.screen.media

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.BatchCompleteContent
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.GlassDialog
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaSwipeScreen(
    yearMonth: String,
    onBackClick: () -> Unit,
    onMediaClick: (Long) -> Unit,
    viewModel: MediaSwipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val canUndo by viewModel.undoManager.canUndo.collectAsState()
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeight.toDp() }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.items.size }
    )
    var lastPage by remember { mutableStateOf<Int?>(null) }
    var isSwipingKeptPhoto = false

    LaunchedEffect(uiState.currentIndex, uiState.items.size) {
        if (uiState.items.isNotEmpty()) {
            val safeIndex = uiState.currentIndex.coerceIn(0, uiState.items.size - 1)
            if (pagerState.currentPage != safeIndex) {
                pagerState.scrollToPage(safeIndex)
            }
        }
    }

    LaunchedEffect(uiState.deleteMessage) {
        val message = uiState.deleteMessage
        if (message.isNotEmpty()) {
            val result = snackbarHostState.showSnackbar(message)
            if (result == androidx.compose.material3.SnackbarResult.Dismissed ||
                result == androidx.compose.material3.SnackbarResult.ActionPerformed
            ) {
                viewModel.dismissResultMessage()
            }
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

    LaunchedEffect(Unit) {
        com.gallery.cleaner.MainActivity.onTrashResult = { success, count ->
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
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "加载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.Destructive
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        uiState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }

            uiState.isBatchComplete -> {
                BatchCompleteContent(
                    batchTotal = uiState.batchTotal,
                    keptCount = uiState.keptCount,
                    queuedCount = uiState.deleteQueue.items.size,
                    onConfirmDelete = { viewModel.showDeleteConfirmDialog() },
                    onExit = {
                        viewModel.onBackPressed()
                        onBackClick()
                    }
                )
            }

            else -> {
                val browseProgress = if (uiState.items.isNotEmpty()) {
                    (uiState.currentIndex + 1).toFloat() / uiState.items.size.toFloat()
                } else 0f

                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(topBarHeightDp))

                    SwipeStatusStrip(
                        currentIndex = uiState.currentIndex + 1,
                        totalCount = uiState.items.size,
                        queuedCount = uiState.deleteQueue.items.size
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
                        contentPadding = PaddingValues(0.dp),
                        beyondBoundsPageCount = 1
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
                .zIndex(1f)
                .onGloballyPositioned { topBarHeight = it.size.height }
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = yearMonth.replace("-", "年") + "月",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "已选 ${uiState.deleteQueue.items.size}/${DeleteQueue.MAX_QUEUE_SIZE}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackPressed(); onBackClick() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                actions = {},
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 200.dp)
                .zIndex(3f)
        )

        if (uiState.showDeleteDialog) {
            val deleteMessage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                "确定要将 ${uiState.deleteQueue.items.size} 个项目移入系统回收站吗？可在应用内回收站或系统相册的\"最近删除\"中查看和恢复。"
            } else {
                "确定要永久删除 ${uiState.deleteQueue.items.size} 个项目吗？当前系统版本不支持回收站功能，删除后无法恢复。"
            }
            GlassDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                title = "确认删除",
                text = deleteMessage,
                confirmText = "删除",
                isDestructive = true,
                onConfirm = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }
    }
}

@Composable
private fun SwipeStatusStrip(
    currentIndex: Int,
    totalCount: Int,
    queuedCount: Int
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppPadding.LG, vertical = AppPadding.SM),
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
                    text = "SWIPE BOARD",
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
                    text = "左滑自动归类 · 待删 $queuedCount / ${DeleteQueue.MAX_QUEUE_SIZE}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}
