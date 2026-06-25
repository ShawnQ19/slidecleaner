package com.gallery.cleaner.ui.screen.random

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.gallery.cleaner.MainActivity
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.GlassDialog
import com.gallery.cleaner.ui.component.GlassSnackbar
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.component.SnackbarType
import com.gallery.cleaner.ui.screen.media.DeleteQueueBar
import com.gallery.cleaner.ui.screen.media.SwipeableMediaCard
import com.gallery.cleaner.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RandomCleanupScreen(
    onBackClick: () -> Unit,
    onMediaClick: (Long) -> Unit,
    viewModel: RandomCleanupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.undoManager.canUndo.collectAsState()
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeight.toDp() }

    val visibleItems = uiState.visibleItems
    val displayIndexMap = uiState.displayIndexMap
    val pageCount = visibleItems.size + 1

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    var lastPage by remember { mutableStateOf<Int?>(null) }

    val currentDisplayIndex = if (pagerState.currentPage < visibleItems.size) {
        displayIndexMap.getOrElse(pagerState.currentPage) { uiState.batchTotal }
    } else {
        uiState.batchTotal
    }

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
        } else if (currentPage == visibleItems.size) {
            if (previousPage != null && previousPage < visibleItems.size) {
                viewModel.keepCurrent()
            }
        }
        lastPage = currentPage
    }

    LaunchedEffect(uiState.deleteMessage) {
        val message = uiState.deleteMessage
        if (message.isNotBlank()) {
            snackbarMessage = message
            showSnackbar = true
            kotlinx.coroutines.delay(3000)
            showSnackbar = false
            kotlinx.coroutines.delay(300)
            viewModel.dismissResultMessage()
        }
    }

    LaunchedEffect(Unit) {
        MainActivity.onTrashResult = { success, count -> viewModel.onTrashIntentResult(success, count) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppPadding.MD)) {
                        CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp)
                        Text(text = "正在随机抽取...", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                    }
                }
            }
            uiState.error != null -> {
                ErrorState(message = uiState.error ?: "加载失败", onRetry = { viewModel.refreshBatch() })
            }
            else -> {
                val browseProgress = if (uiState.batchTotal > 0) currentDisplayIndex.toFloat() / uiState.batchTotal.toFloat() else 1f
                val animatedProgress by animateFloatAsState(targetValue = browseProgress.coerceIn(0f, 1f), animationSpec = tween(300), label = "browseProgress")

                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(topBarHeightDp))
                    RandomStatusStrip(currentIndex = currentDisplayIndex, totalCount = uiState.batchTotal, queuedCount = uiState.deleteQueue.items.size, processedCount = uiState.processedCount, modifier = Modifier.padding(horizontal = AppPadding.LG, vertical = AppPadding.SM))
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).padding(horizontal = AppPadding.LG).clip(AppShape.Pill).background(AppColors.SurfaceOverlay)) {
                        Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxSize().clip(AppShape.Pill).background(AppColors.Primary))
                    }
                    val isOnEndPage = pagerState.currentPage >= visibleItems.size
                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                        if (page < visibleItems.size) {
                            val item = visibleItems[page]
                            SwipeableMediaCard(mediaItem = item, onSwipeUp = {
                                if (viewModel.isItemKept(item)) viewModel.unkeepCurrent(item)
                                viewModel.queueForDelete(item)
                            }, onTap = { onMediaClick(item.id) }, isKept = viewModel.isItemKept(item), modifier = Modifier.fillMaxSize())
                        } else {
                            EndPageContent(batchTotal = uiState.batchTotal, keptCount = uiState.keptCount, queuedCount = uiState.deleteQueue.items.size, onConfirmDelete = { viewModel.showDeleteConfirmDialog() }, onExit = { viewModel.showExitConfirmDialog() })
                        }
                    }
                    if (!isOnEndPage) {
                        DeleteQueueBar(deleteQueue = uiState.deleteQueue, onConfirmDelete = { viewModel.showDeleteConfirmDialog() }, onUndo = { viewModel.undo() }, canUndo = canUndo)
                    }
                }
            }
        }

        GlassTopBar(modifier = Modifier.fillMaxWidth().zIndex(1f).onGloballyPositioned { topBarHeight = it.size.height }) {
            TopAppBar(title = {
                Column {
                    Text(text = "随机清理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text(text = "已整理 ${uiState.processedCount} 项 · 本轮 ${uiState.batchTotal} 张", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                }
            }, navigationIcon = {
                IconButton(onClick = { viewModel.onBackPressed(); onBackClick() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = AppColors.TextPrimary) }
            }, actions = {
                IconButton(onClick = { viewModel.refreshBatch() }) { Icon(Icons.Default.Shuffle, "重新随机", tint = AppColors.TextSecondary) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = AppColors.TextPrimary, navigationIconContentColor = AppColors.TextPrimary, actionIconContentColor = AppColors.TextPrimary), modifier = Modifier.statusBarsPadding())
        }

        GlassSnackbar(visible = showSnackbar, message = snackbarMessage, type = if (snackbarMessage.contains("成功") || snackbarMessage.contains("移入")) SnackbarType.SUCCESS else SnackbarType.ERROR, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.dp).zIndex(3f))

        if (uiState.showDeleteDialog && uiState.deleteQueue.items.isNotEmpty()) {
            GlassDialog(onDismissRequest = { viewModel.dismissDeleteDialog() }, title = "确认删除", text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) "确定要将 ${uiState.deleteQueue.items.size} 个项目移入系统回收站吗？" else "确定要永久删除 ${uiState.deleteQueue.items.size} 个项目吗？", confirmText = "删除", isDestructive = true, onConfirm = { viewModel.confirmDelete() }, onDismiss = { viewModel.dismissDeleteDialog() })
        }

        if (uiState.showPostDeleteDialog) {
            GlassDialog(onDismissRequest = { viewModel.dismissPostDeleteDialog() }, title = "删除完成", text = "${uiState.deleteMessage}，是否进行下一批随机整理？", confirmText = "下一批", dismissText = "返回首页", isDestructive = false, onConfirm = { viewModel.dismissPostDeleteDialog(); viewModel.loadNextBatch() }, onDismiss = { viewModel.dismissPostDeleteDialog(); viewModel.onBackPressed(); onBackClick() })
        }

        if (uiState.showExitConfirmDialog) {
            GlassDialog(onDismissRequest = { viewModel.dismissExitConfirmDialog() }, title = "确认退出", text = "返回首页将不会删除任何照片，确定退出吗？", confirmText = "退出", isDestructive = true, onConfirm = { viewModel.dismissExitConfirmDialog(); viewModel.clearDeleteQueue(); viewModel.onBackPressed(); onBackClick() }, onDismiss = { viewModel.dismissExitConfirmDialog() })
        }
    }
}

@Composable
private fun EndPageContent(batchTotal: Int, keptCount: Int, queuedCount: Int, onConfirmDelete: () -> Unit, onExit: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = AppPadding.XXL), contentAlignment = Alignment.Center) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = AppShape.XLarge, contentPadding = AppPadding.XXL) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppPadding.MD)) {
                Text(text = "SWIPE SEQUENCE COMPLETE", style = MaterialTheme.typography.labelSmall, color = AppColors.TextTertiary, fontWeight = FontWeight.SemiBold)
                Text(text = "本轮清理完成", style = MaterialTheme.typography.headlineMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(AppPadding.LG), verticalAlignment = Alignment.CenterVertically) {
                    StatBadge(label = "总数", value = "$batchTotal")
                    StatBadge(label = "保留", value = "$keptCount", color = AppColors.Success)
                    if (queuedCount > 0) StatBadge(label = "删除", value = "$queuedCount", color = AppColors.Destructive)
                }
                Spacer(modifier = Modifier.height(AppPadding.SM))
                if (queuedCount > 0) {
                    Button(onClick = onConfirmDelete, modifier = Modifier.fillMaxWidth(), shape = AppShape.Medium, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Destructive), contentPadding = PaddingValues(vertical = 14.dp)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("确认删除 ($queuedCount 张)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp), tint = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("返回首页", color = AppColors.TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color = AppColors.Primary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Black)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextTertiary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RandomStatusStrip(currentIndex: Int, totalCount: Int, queuedCount: Int, processedCount: Int, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth(), shape = AppShape.XLarge, contentPadding = AppPadding.MD) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "RANDOM BOARD", style = MaterialTheme.typography.labelSmall, color = AppColors.TextTertiary, fontWeight = FontWeight.SemiBold)
                Text(text = "第 $currentIndex / $totalCount 张", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                Text(text = "左滑自动归类 · 已整理 $processedCount 项", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppPadding.XS)) {
                StatusChip(label = "QUEUE $queuedCount", tint = if (queuedCount >= DeleteQueue.MAX_QUEUE_SIZE) AppColors.Destructive else AppColors.Primary)
                StatusChip(label = "READY", tint = AppColors.Accent)
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, tint: Color) {
    Box(modifier = Modifier.clip(AppShape.Pill).background(tint.copy(alpha = 0.14f)).border(1.dp, tint.copy(alpha = 0.24f), AppShape.Pill).padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = AppPadding.XXL), contentAlignment = Alignment.Center) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = AppShape.XLarge, contentPadding = AppPadding.XXL) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppPadding.SM)) {
                Text(text = "加载失败", style = MaterialTheme.typography.titleMedium, color = AppColors.Destructive, fontWeight = FontWeight.SemiBold)
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                Button(onClick = onRetry) { Text("重试") }
            }
        }
    }
}
