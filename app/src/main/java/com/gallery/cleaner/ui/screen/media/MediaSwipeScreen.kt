package com.gallery.cleaner.ui.screen.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.BatchCompleteContent
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.GlassDialog
import com.gallery.cleaner.ui.component.GlassSnackbar
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.component.SnackbarType
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
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    val canUndo by viewModel.undoManager.canUndo.collectAsState()
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeight.toDp() }

    val visibleItems = uiState.visibleItems
    val pageCount = visibleItems.size + 1

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pageCount }
    )
    var lastPage by remember { mutableStateOf<Int?>(null) }
    var browsedCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(visibleItems.size, uiState.currentIndex) {
        val target = uiState.currentIndex
        if (target in 0 until visibleItems.size && pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        val previousPage = lastPage

        if (currentPage < visibleItems.size) {
            if (previousPage != null && currentPage > previousPage && previousPage < visibleItems.size) {
                viewModel.keepCurrent()
            }
            viewModel.setCurrentIndex(currentPage)
            if (currentPage + 1 > browsedCount) browsedCount = currentPage + 1
        } else if (currentPage == visibleItems.size) {
            if (previousPage != null && previousPage < visibleItems.size) {
                viewModel.keepCurrent()
            }
        }

        lastPage = currentPage
    }

    LaunchedEffect(uiState.deleteMessage) {
        val message = uiState.deleteMessage
        if (message.isNotEmpty()) {
            snackbarMessage = message
            showSnackbar = true
            kotlinx.coroutines.delay(3000)
            showSnackbar = false
            kotlinx.coroutines.delay(300)
            viewModel.dismissResultMessage()
        }
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
                    Text("加载失败", style = MaterialTheme.typography.titleMedium, color = AppColors.Destructive)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.error ?: "", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                }
            }

            else -> {
                val browseProgress = if (uiState.batchTotal > 0) {
                    browsedCount.toFloat() / uiState.batchTotal.toFloat()
                } else 1f

                val animatedProgress by animateFloatAsState(
                    targetValue = browseProgress.coerceIn(0f, 1f),
                    animationSpec = tween(300),
                    label = "browseProgress"
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(topBarHeightDp))

                    SwipeStatusStrip(
                        currentIndex = browsedCount.coerceAtMost(uiState.batchTotal),
                        totalCount = uiState.batchTotal,
                        queuedCount = uiState.deleteQueue.items.size
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.Primary,
                        trackColor = AppColors.SurfaceElevated
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        if (page < visibleItems.size) {
                            val item = visibleItems[page]
                            SwipeableMediaCard(
                                mediaItem = item,
                                onSwipeUp = {
                                    if (viewModel.isItemKept(item)) {
                                        viewModel.unkeepCurrent(item)
                                    }
                                    viewModel.queueForDelete(item)
                                },
                                onTap = { onMediaClick(item.id) },
                                isKept = viewModel.isItemKept(item),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
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
            modifier = Modifier.fillMaxWidth().zIndex(1f).onGloballyPositioned { topBarHeight = it.size.height }
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(yearMonth.replace("-", "年") + "月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("已选 ${uiState.deleteQueue.items.size}/${DeleteQueue.MAX_QUEUE_SIZE}", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackPressed(); onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = AppColors.TextPrimary)
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = AppColors.TextPrimary, navigationIconContentColor = AppColors.TextPrimary, actionIconContentColor = AppColors.TextPrimary),
                modifier = Modifier.statusBarsPadding()
            )
        }

        GlassSnackbar(
            visible = showSnackbar,
            message = snackbarMessage,
            type = if (snackbarMessage.contains("成功") || snackbarMessage.contains("移入")) SnackbarType.SUCCESS else SnackbarType.ERROR,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.dp).zIndex(3f)
        )

        if (uiState.showDeleteDialog) {
            val deleteMessage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                "确定要将 ${uiState.deleteQueue.items.size} 个项目移入系统回收站吗？可在应用内回收站或系统相册的\"最近删除\"中查看和恢复。"
            } else {
                "确定要永久删除 ${uiState.deleteQueue.items.size} 个项目吗？当前系统版本不支持回收站功能，删除后无法恢复。"
            }
            GlassDialog(onDismissRequest = { viewModel.dismissDeleteDialog() }, title = "确认删除", text = deleteMessage, confirmText = "删除", isDestructive = true, onConfirm = { viewModel.confirmDelete() }, onDismiss = { viewModel.dismissDeleteDialog() })
        }
    }
}

@Composable
private fun SwipeStatusStrip(currentIndex: Int, totalCount: Int, queuedCount: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = AppPadding.LG, vertical = AppPadding.SM), shape = AppShape.XLarge, contentPadding = AppPadding.MD) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "SWIPE BOARD", style = MaterialTheme.typography.labelSmall, color = AppColors.TextTertiary, fontWeight = FontWeight.SemiBold)
                Text(text = "第 $currentIndex / $totalCount 张", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                Text(text = "左滑自动归类 · 待删 $queuedCount / ${DeleteQueue.MAX_QUEUE_SIZE}", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
            }
        }
    }
}
