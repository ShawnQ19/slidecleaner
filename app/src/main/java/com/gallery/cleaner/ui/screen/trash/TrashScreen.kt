package com.gallery.cleaner.ui.screen.trash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassBottomBar
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.component.Motion
import com.gallery.cleaner.ui.component.Responsive
import com.gallery.cleaner.ui.theme.AppColors

private const val TAG = "TrashScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBackClick: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeight.toDp() }
    var showDeleteForeverDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showRestoreSelectedDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            snackbarHostState.showSnackbar(uiState.message)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(Unit) {
        com.gallery.cleaner.MainActivity.onDeleteResult = { success, count ->
            viewModel.onDeleteIntentResult(success, count)
        }
        com.gallery.cleaner.MainActivity.onRestoreResult = { success, count ->
            viewModel.onRestoreIntentResult(success, count)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppColors.Primary,
                        strokeWidth = 3.dp
                    )
                }
            }

            uiState.error != null -> {
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppPadding.SM)
                        ) {
                            Text(
                                "加载失败",
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.Destructive,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                uiState.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                            Button(onClick = { viewModel.loadTrashItems() }) {
                                Text("重试")
                            }
                        }
                    }
                }
            }

            uiState.trashItems.isEmpty() -> {
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppPadding.MD)
                        ) {
                            Text(
                                text = "RECYCLE BIN ZEROED",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "回收站已清空",
                                style = MaterialTheme.typography.headlineSmall,
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "删除的照片会先进入这里，方便集中恢复或二次清理。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(AppPadding.SM)) {
                                StatusChip(label = "SAFE ARCHIVE", tint = AppColors.Primary)
                                StatusChip(label = "READY", tint = AppColors.Accent)
                            }
                        }
                    }
                }
            }

            else -> {
                val gridState = rememberLazyGridState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeightDp)
                ) {
                    TrashSummaryCard(
                        isSelectMode = uiState.isSelectMode,
                        selectedCount = uiState.selectedItems.size,
                        totalCount = uiState.trashItems.size,
                        modifier = Modifier.padding(
                            start = Responsive.gridContentPadding.calculateLeftPadding(
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                            ),
                            end = Responsive.gridContentPadding.calculateRightPadding(
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                            ),
                            top = Responsive.gridContentPadding.calculateTopPadding(),
                            bottom = AppPadding.SM
                        )
                    )

                    LazyVerticalGrid(
                        columns = Responsive.gridCells(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(uiState.isSelectMode) {
                                if (uiState.isSelectMode) {
                                    detectDragGestures(
                                        onDrag = { change, _ -> change.consume() }
                                    )
                                }
                            },
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = Responsive.gridContentPadding.calculateLeftPadding(
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                            ),
                            end = Responsive.gridContentPadding.calculateRightPadding(
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                            ),
                            bottom = Responsive.gridContentPadding.calculateBottomPadding() +
                                    if (uiState.isSelectMode && uiState.selectedItems.isNotEmpty()) 72.dp else 0.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(Responsive.gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(Responsive.gridSpacing)
                    ) {
                        items(uiState.trashItems, key = { it.id }) { item ->
                            TrashItemCard(
                                item = item,
                                isSelectMode = uiState.isSelectMode,
                                isSelected = item.id in uiState.selectedItems,
                                onLongClick = {
                                    if (!uiState.isSelectMode) viewModel.enterSelectMode(item.id)
                                },
                                onClick = {
                                    if (uiState.isSelectMode) viewModel.toggleSelection(item.id)
                                    else {
                                        selectedItem = item
                                        showRestoreDialog = true
                                    }
                                }
                            )
                        }
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
            if (uiState.isSelectMode) {
                SelectModeTopBar(
                    selectedCount = uiState.selectedItems.size,
                    totalCount = uiState.trashItems.size,
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.exitSelectMode() },
                    onClose = { viewModel.exitSelectMode() }
                )
            } else {
                NormalTopBar(
                    itemCount = uiState.trashItems.size,
                    onBackClick = onBackClick,
                    onClearAll = { showDeleteForeverDialog = true }
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.isSelectMode && uiState.selectedItems.isNotEmpty(),
            enter = Motion.Enter.bottomBarEnter(),
            exit = Motion.Exit.bottomBarExit(),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(2f)
        ) {
            GlassBottomBar {
                BatchActionBar(
                    selectedCount = uiState.selectedItems.size,
                    onRestore = { showRestoreSelectedDialog = true },
                    onDelete = { showDeleteSelectedDialog = true }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f)
        )
    }

    if (showRestoreDialog && selectedItem != null) {
        com.gallery.cleaner.ui.component.GlassDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = "恢复照片",
            text = "确定要恢复这张照片到相册吗？",
            confirmText = "恢复",
            onConfirm = {
                viewModel.restoreFromTrash(selectedItem!!)
                showRestoreDialog = false
                selectedItem = null
            },
            onDismiss = { showRestoreDialog = false }
        )
    }
    if (showDeleteForeverDialog) {
        com.gallery.cleaner.ui.component.GlassDialog(
            onDismissRequest = { showDeleteForeverDialog = false },
            title = "清空回收站",
            text = "确定要永久删除回收站中的 ${uiState.trashItems.size} 个项目吗？此操作不可撤销。",
            confirmText = "永久删除",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteForever()
                showDeleteForeverDialog = false
            },
            onDismiss = { showDeleteForeverDialog = false }
        )
    }
    if (showDeleteSelectedDialog) {
        com.gallery.cleaner.ui.component.GlassDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = "永久删除",
            text = "确定要永久删除选中的 ${uiState.selectedItems.size} 个项目吗？此操作不可撤销。",
            confirmText = "永久删除",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSelected()
                showDeleteSelectedDialog = false
            },
            onDismiss = { showDeleteSelectedDialog = false }
        )
    }
    if (showRestoreSelectedDialog) {
        com.gallery.cleaner.ui.component.GlassDialog(
            onDismissRequest = { showRestoreSelectedDialog = false },
            title = "恢复项目",
            text = "确定要恢复选中的 ${uiState.selectedItems.size} 个项目到相册吗？",
            confirmText = "恢复",
            onConfirm = {
                                viewModel.restoreSelected()
                showRestoreSelectedDialog = false
            },
            onDismiss = { showRestoreSelectedDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalTopBar(
    itemCount: Int,
    onBackClick: () -> Unit,
    onClearAll: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "回收站",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    "RECYCLE BIN · $itemCount 项",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = AppColors.TextPrimary
                )
            }
        },
        actions = {
            if (itemCount > 0) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "清空回收站",
                        tint = AppColors.Destructive
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectModeTopBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "批量选择",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    "SELECT MODE · $selectedCount / $totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "取消选择",
                    tint = AppColors.TextPrimary
                )
            }
        },
        actions = {
            if (selectedCount < totalCount) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "全选",
                        tint = AppColors.Primary
                    )
                }
            } else {
                IconButton(onClick = onDeselectAll) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "取消全选",
                        tint = AppColors.TextSecondary
                    )
                }
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

@Composable
private fun BatchActionBar(
    selectedCount: Int,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onRestore,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
            shape = AppShape.Medium,
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("恢复 ($selectedCount)", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Destructive),
            shape = AppShape.Medium,
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("删除 ($selectedCount)", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TrashSummaryCard(
    isSelectMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = AppShape.XLarge,
        contentPadding = AppPadding.LG
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppPadding.MD)) {
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
                        text = if (isSelectMode) "BATCH SELECT" else "RECYCLE BIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextTertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isSelectMode) "已选择 $selectedCount 项" else "回收站",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isSelectMode) "批量恢复 / 永久删除" else "可恢复、可批量处理、可永久删除",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary
                    )
                }

                StatusChip(
                    label = if (isSelectMode) "SELECTED" else "ARCHIVE",
                    tint = if (isSelectMode) AppColors.Accent else AppColors.Primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppPadding.SM)) {
                TrashMetricTile(
                    label = "TOTAL",
                    value = totalCount.toString(),
                    tint = AppColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                TrashMetricTile(
                    label = "SELECTED",
                    value = selectedCount.toString(),
                    tint = if (selectedCount > 0) AppColors.Accent else AppColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                TrashMetricTile(
                    label = "STATE",
                    value = if (isSelectMode) "LOCKED" else "READY",
                    tint = if (isSelectMode) AppColors.Accent else AppColors.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(AppShape.Pill)
            .background(tint.copy(alpha = 0.14f))
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
private fun TrashMetricTile(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShape.Medium)
            .background(AppColors.SurfaceElevated.copy(alpha = 0.82f))
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
private fun TrashItemCard(
    item: MediaItem,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(0.95f, spring(stiffness = Spring.StiffnessHigh))
            scale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(AppShape.Small)
            .then(
                if (isSelected) Modifier.border(2.5.dp, AppColors.Primary, AppShape.Small)
                else Modifier
            )
            .pointerInput(isSelectMode) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = Motion.Enter.scaleIn(initialScale = 0.5f) + Motion.Enter.fadeIn(),
            exit = Motion.Exit.scaleOut(targetScale = 0.5f) + Motion.Exit.fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Primary.copy(alpha = 0.18f))
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = Motion.Enter.scaleIn(initialScale = 0.3f) + Motion.Enter.fadeIn(),
            exit = Motion.Exit.scaleOut(targetScale = 0.3f) + Motion.Exit.fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (isSelectMode && !isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.25.dp, AppColors.TextTertiary, CircleShape)
            )
        }

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
