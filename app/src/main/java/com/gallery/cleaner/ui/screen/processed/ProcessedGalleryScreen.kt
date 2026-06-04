package com.gallery.cleaner.ui.screen.processed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.GlassDialog
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.component.Motion
import com.gallery.cleaner.ui.component.Responsive
import com.gallery.cleaner.ui.component.pressClick
import com.gallery.cleaner.ui.permission.PermissionHandler
import com.gallery.cleaner.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessedGalleryScreen(
    onBackClick: () -> Unit,
    onMonthClick: (String) -> Unit,
    viewModel: ProcessedGalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }
    val topBarHeightDp = with(density) { topBarHeight.toDp() }

    PermissionHandler(
        onPermissionGranted = {
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
                            text = "已整理为空",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "被保留或确认删除的媒体会自动汇入这里，并按月份分组。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(onClick = onBackClick) {
                            Text("返回相册")
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
                            ProcessedGalleryHeroPanel(
                                totalCount = totalCount,
                                monthCount = monthCount,
                                averagePerMonth = averagePerMonth,
                                onResetClick = { showResetDialog = true }
                            )
                        }

                        itemsIndexed(
                            uiState.groups,
                            key = { _, group -> group.yearMonth.toString() }
                        ) { index, group ->
                            com.gallery.cleaner.ui.screen.gallery.MonthCard(
                                group = group,
                                onClick = { onMonthClick(group.yearMonth.toString()) },
                                index = index
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.isLoading,
                        enter = Motion.Enter.fadeIn(),
                        exit = Motion.Exit.fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppColors.Primary)
                        }
                    }
                }
            }

            if (showResetDialog) {
                GlassDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = "重置已整理",
                    text = "这会把所有已整理项目恢复为未整理状态，普通相册会重新显示这些项目。",
                    confirmText = "重置",
                    isDestructive = true,
                    onConfirm = {
                        viewModel.resetAllProcessed()
                        showResetDialog = false
                    },
                    onDismiss = { showResetDialog = false }
                )
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
                                text = "已整理",
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
                        val totalCount = uiState.groups.sumOf { it.mediaItems.size }
                        if (totalCount > 0) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.Accent,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "$totalCount 个项目",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextTertiary,
                                modifier = Modifier.padding(end = 16.dp)
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
        }
    }
}

@Composable
private fun ProcessedGalleryHeroPanel(
    totalCount: Int,
    monthCount: Int,
    averagePerMonth: Int,
    onResetClick: () -> Unit
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
                    text = "SCENE 02 · ARCHIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(AppShape.Pill)
                        .background(AppColors.Accent.copy(alpha = 0.14f))
                        .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)
                ) {
                    Text(
                        text = if (totalCount > 0) "READY" else "EMPTY",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "已整理",
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "这里展示所有已归类的照片和视频，按月份分组。",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppPadding.SM)
            ) {
                StatTile(
                    label = "总项目",
                    value = totalCount.toString()
                )
                StatTile(
                    label = "月份",
                    value = monthCount.toString()
                )
                StatTile(
                    label = "月均",
                    value = if (monthCount > 0) averagePerMonth.toString() else "0"
                )
            }

            if (totalCount > 0) {
                Button(
                    onClick = onResetClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShape.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Destructive,
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "重置已整理",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
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