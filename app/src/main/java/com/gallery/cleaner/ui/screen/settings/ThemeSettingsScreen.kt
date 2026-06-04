package com.gallery.cleaner.ui.screen.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.gallery.cleaner.domain.model.ThemePreset
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.GlassTopBar
import com.gallery.cleaner.ui.component.pressClick
import com.gallery.cleaner.ui.theme.AppColors
import com.gallery.cleaner.ui.theme.GlassColors

private const val TAG = "ThemeSettingsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel()
) {
    val themeConfig by viewModel.themeConfig.collectAsState()
    val context = LocalContext.current

    val colorPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getIntExtra("color", 0)?.let { colorInt ->
                viewModel.setCustomColor(Color(colorInt))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
                .padding(horizontal = AppPadding.LG),
            verticalArrangement = Arrangement.spacedBy(AppPadding.LG)
        ) {
            Text(
                text = "选择主题色",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(AppPadding.MD),
                verticalArrangement = Arrangement.spacedBy(AppPadding.MD),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ThemePreset.entries) { preset ->
                    PresetColorItem(
                        preset = preset,
                        isSelected = themeConfig.presetName == preset.name,
                        onClick = { viewModel.setPreset(preset) }
                    )
                }
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressClick {
                        try {
                            val intent = Intent("com.android.colorpicker.PICK_COLOR")
                            intent.putExtra("color", themeConfig.primaryColor.hashCode())
                            colorPickerLauncher.launch(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Intent.ACTION_MAIN)
                                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                                colorPickerLauncher.launch(intent)
                            } catch (_: Exception) {
                            }
                        }
                    },
                shape = AppShape.Medium,
                contentPadding = AppPadding.LG
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(AppPadding.MD))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "自定义颜色",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "使用系统取色器选择任意颜色",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.TextTertiary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = AppColors.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppPadding.LG))

            PreviewSection(primaryColor = themeConfig.primaryColor)
        }

        GlassTopBar(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "主题设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
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
                    navigationIconContentColor = AppColors.TextPrimary,
                    actionIconContentColor = AppColors.TextPrimary
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    }
}

@Composable
private fun PresetColorItem(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) preset.color else Color.Transparent,
        animationSpec = tween(200),
        label = "border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "borderWidth"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(preset.color)
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = preset.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) preset.color else AppColors.TextTertiary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PreviewSection(primaryColor: Color) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.Large,
        contentPadding = AppPadding.LG
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppPadding.MD)) {
            Text(
                text = "效果预览",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppPadding.SM)
            ) {
                listOf(1f, 0.7f, 0.4f, 0.15f).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(AppShape.Small)
                            .background(primaryColor.copy(alpha = alpha))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(AppShape.Small)
                    .background(primaryColor),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "主题色按钮",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
