package com.gallery.cleaner.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallery.cleaner.ui.theme.AppColors

enum class SnackbarType {
    SUCCESS,
    ERROR
}

@Composable
fun GlassSnackbar(
    visible: Boolean,
    message: String,
    type: SnackbarType = SnackbarType.SUCCESS,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = spring(stiffness = Spring.StiffnessHigh)
        ) + fadeOut(),
        modifier = modifier
    ) {
        val backgroundColor = when (type) {
            SnackbarType.SUCCESS -> Color(0xFF1A2D1F)
            SnackbarType.ERROR -> Color(0xFF2D1A1A)
        }
        val borderColor = when (type) {
            SnackbarType.SUCCESS -> Color(0xFF42D8A0).copy(alpha = 0.3f)
            SnackbarType.ERROR -> Color(0xFFFF6C7C).copy(alpha = 0.3f)
        }
        val iconColor = when (type) {
            SnackbarType.SUCCESS -> Color(0xFF42D8A0)
            SnackbarType.ERROR -> Color(0xFFFF6C7C)
        }
        val icon = when (type) {
            SnackbarType.SUCCESS -> Icons.Filled.CheckCircle
            SnackbarType.ERROR -> Icons.Filled.Error
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppPadding.LG, vertical = AppPadding.SM)
                .clip(AppShape.Medium)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor,
                                backgroundColor.copy(alpha = 0.95f)
                            )
                        )
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .border(1.dp, borderColor, AppShape.Medium)
                .padding(horizontal = AppPadding.LG, vertical = AppPadding.MD)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppPadding.MD),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )

                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) {
                        Text(
                            text = actionLabel,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}