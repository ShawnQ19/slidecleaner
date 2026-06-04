package com.gallery.cleaner.ui.screen.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.component.Motion
import com.gallery.cleaner.ui.component.Responsive
import com.gallery.cleaner.ui.permission.PermissionHandler
import com.gallery.cleaner.ui.theme.AppColors
import com.gallery.cleaner.util.log.AppLogger

private const val TAG = "GalleryScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onMonthClick: (String) -> Unit,
    onProcessedClick: () -> Unit = {},
    onRandomCleanupClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onThemeSettingsClick: () -> Unit = {},
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeight.toDp() }

    AppLogger.d(TAG, "GalleryScreen 重组，groups=${uiState.groups.size}")

    PermissionHandler(
        onPermissionGranted = {
            AppLogger.d(TAG, "PermissionHandler onPermissionGranted")
            viewModel.onPermissionGranted()
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isInitialLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppColors.Primary
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.Destructive
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(onClick = { viewModel.retry() }) {
                            Text("重试")
                        }
                    }
                }
                !uiState.isLoading && uiState.groups.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "没有找到照片或视频",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(onClick = { viewModel.retry() }) {
                            Text("刷新")
                        }
                    }
                }
                else -> {
                    val totalCount = uiState.groups.sumOf { it.mediaItems.size }
                    val monthCount = uiState.groups.size
                    val averagePerMonth = if (monthCount > 0) totalCount / monthCount else 0

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Responsive.listContentPadding.calculateLeftPadding(
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                            ),
                            end = Responsive.listContentPadding.calculateRightPadding(
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                            ),
                            top = Responsive.listContentPadding.calculateTopPadding() + topBarHeightDp,
                            bottom = Responsive.listContentPadding.calculateBottomPadding()
                        ),
                        verticalArrangement = Arrangement.spacedBy(Responsive.listSpacing)
                    ) {
                        item(key = "hero") {
                            GalleryHeroPanel(
                                totalCount = totalCount,
                                monthCount = monthCount,
                                averagePerMonth = averagePerMonth,
                                onProcessedClick = onProcessedClick,
                                onRandomCleanupClick = onRandomCleanupClick
                            )
                        }

                        itemsIndexed(
                            uiState.groups,
                            key = { _, group -> group.yearMonth.toString() }
                        ) { index, group ->
                            MonthCard(
                                group = group,
                                onClick = { onMonthClick(group.yearMonth.toString()) },
                                index = index
                            )
                        }
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
                                text = "相册清理",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            if (uiState.groups.isNotEmpty()) {
                                Text(
                                    text = "${uiState.groups.size} 个月份",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = AppColors.TextPrimary,
                        navigationIconContentColor = AppColors.TextPrimary,
                        actionIconContentColor = AppColors.TextSecondary
                    ),
                    actions = {
                        IconButton(onClick = {
                            AppLogger.userAction(TAG, "点击主题设置")
                            onThemeSettingsClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "主题设置",
                                tint = AppColors.TextSecondary
                            )
                        }
                        IconButton(onClick = {
                            AppLogger.userAction(TAG, "点击回收站入口")
                            onTrashClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "回收站",
                                tint = AppColors.TextSecondary
                            )
                        }
                        val totalCount = uiState.groups.sumOf { it.mediaItems.size }
                        Text(
                            text = "$totalCount 个项目",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.TextTertiary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun GalleryHeroPanel(
    totalCount: Int,
    monthCount: Int,
    averagePerMonth: Int,
    onProcessedClick: () -> Unit,
    onRandomCleanupClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.XLarge,
        contentPadding = AppPadding.LG
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppPadding.MD)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCENE 01 · DEEP BLUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(AppShape.Pill)
                        .background(AppColors.Primary.copy(alpha = 0.14f))
                        .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)
                ) {
                    Text(
                        text = if (totalCount > 0) "READY" else "WAITING",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "相册清理",
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "按月浏览设备媒体 · 批量加入删除队列 · 系统回收站保护",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(AppShape.Pill)
                    .background(AppColors.SurfaceOverlay.copy(alpha = 0.8f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (totalCount > 0) 0.78f else 0.18f)
                        .fillMaxSize()
                        .clip(AppShape.Pill)
                        .background(AppColors.Primary)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppPadding.SM)
            ) {
                StatTile(label = "总项目", value = totalCount.toString())
                StatTile(label = "月份", value = monthCount.toString())
                StatTile(label = "月均", value = if (monthCount > 0) averagePerMonth.toString() else "0")
            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppPadding.SM)
                            ) {
                                Button(
                                    onClick = onProcessedClick,
                                    modifier = Modifier.weight(1f),
                                    shape = AppShape.Medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.SurfaceElevated,
                                        contentColor = AppColors.TextPrimary
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = "已整理",
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary
                                    )
                                }

                                Button(
                                    onClick = onRandomCleanupClick,
                                    modifier = Modifier.weight(1f),
                                    shape = AppShape.Medium,
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shuffle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = "随机清理 50 张",
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary
                                    )
                                }
                            }

                            Text(
                                text = "随机从全局相册抽取 50 张，处理后会进入已整理，并从普通月份相册里自动消失",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextTertiary
                            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 96.dp)
            .clip(AppShape.Medium)
            .background(AppColors.SurfaceElevated.copy(alpha = 0.78f))
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
                color = AppColors.TextTertiary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
