package com.gallery.cleaner.ui.screen.processed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.component.Responsive
import com.gallery.cleaner.ui.component.MediaThumbnail
import com.gallery.cleaner.ui.component.pressClick
import com.gallery.cleaner.ui.theme.AppColors
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessedMonthScreen(
    yearMonth: String,
    onBackClick: () -> Unit,
    onMediaClick: (Long) -> Unit,
    viewModel: ProcessedMonthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeight.toDp() }
    val gridState = rememberLazyGridState()

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
                    Spacer(modifier = Modifier.height(AppPadding.MD))
                    androidx.compose.material3.Button(onClick = { viewModel.retry() }) {
                        Text("重试")
                    }
                }
            }
            uiState.mediaItems.isEmpty() -> {
                GlassCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppPadding.LG),
                    shape = AppShape.XLarge,
                    contentPadding = AppPadding.XXL
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppPadding.MD),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "这个月份还没有已整理项目",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "回到已整理主页，看看其他月份。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                        androidx.compose.material3.Button(onClick = onBackClick) {
                            Text("返回")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeightDp)
                ) {
                    GridSummaryCard(
                        yearMonth = yearMonth,
                        totalCount = uiState.mediaItems.size,
                        modifier = Modifier.padding(
                            start = Responsive.gridContentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            end = Responsive.gridContentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            top = Responsive.gridContentPadding.calculateTopPadding(),
                            bottom = AppPadding.SM
                        )
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(Responsive.gridColumns),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = Responsive.gridContentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            end = Responsive.gridContentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            bottom = Responsive.gridContentPadding.calculateBottomPadding()
                        ),
                        horizontalArrangement = Arrangement.spacedBy(Responsive.gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(Responsive.gridSpacing)
                    ) {
                        items(uiState.mediaItems, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(AppShape.Medium)
                                    .border(1.dp, AppColors.Separator, AppShape.Medium)
                                    .pressClick(onClick = { onMediaClick(item.id) })
                            ) {
                                MediaThumbnail(
                                    mediaItem = item,
                                    modifier = Modifier.fillMaxSize(),
                                    size = 360
                                )
                            }
                        }
                    }
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
                            text = formatYearMonth(yearMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "已整理 ${uiState.mediaItems.size} 项",
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

@Composable
private fun GridSummaryCard(
    yearMonth: String,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = AppShape.XLarge,
        contentPadding = AppPadding.MD
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ARCHIVE GRID",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextTertiary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatYearMonth(yearMonth),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "$totalCount 个已整理项目",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}

private fun formatYearMonth(yearMonth: String): String {
    return runCatching {
        val parsed = java.time.YearMonth.parse(yearMonth)
        "${parsed.year}年${parsed.monthValue}月"
    }.getOrDefault(yearMonth)
}