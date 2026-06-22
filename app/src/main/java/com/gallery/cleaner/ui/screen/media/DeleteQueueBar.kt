package com.gallery.cleaner.ui.screen.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassBottomBar
import com.gallery.cleaner.ui.theme.AppColors

@Composable
fun DeleteQueueBar(
    deleteQueue: DeleteQueue,
    onConfirmDelete: () -> Unit,
    onKeep: (() -> Unit)? = null,
    onUndo: (() -> Unit)? = null,
    canUndo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = deleteQueue.items.size.toFloat() / DeleteQueue.MAX_QUEUE_SIZE
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "progress"
    )
    val progressColor by animateColorAsState(
        targetValue = if (progress >= 1f) AppColors.Destructive else AppColors.Primary,
        animationSpec = tween(300),
        label = "progressColor"
    )
    val isFull = progress >= 1f

    GlassBottomBar(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                        text = "DELETE QUEUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextTertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "向上滑动加入待删除队列",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "达到 50 项时自动触发确认",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextTertiary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isFull) AppColors.Destructive.copy(alpha = 0.16f)
                            else AppColors.Primary.copy(alpha = 0.14f)
                        )
                        .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column {
                            Text(
                                text = "${deleteQueue.items.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isFull) AppColors.Destructive else AppColors.Primary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "STAGED",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (isFull) AppColors.Destructive else AppColors.Primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppPadding.SM))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(AppShape.Pill)
                    .background(AppColors.SurfaceOverlay)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(5.dp)
                        .clip(AppShape.Pill)
                        .background(progressColor)
                )
            }

            Spacer(modifier = Modifier.height(AppPadding.SM))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppPadding.SM)
            ) {
                if (onKeep != null) {
                    Button(
                        onClick = onKeep,
                        modifier = Modifier.weight(1f),
                        shape = AppShape.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Success,
                            contentColor = Color.White
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "保留",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = AppPadding.SM),
                            color = Color.White
                        )
                    }
                }

                if (onUndo != null) {
                    Button(
                        onClick = onUndo,
                        modifier = Modifier.weight(1f),
                        enabled = canUndo,
                        shape = AppShape.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.SurfaceElevated,
                            contentColor = AppColors.TextPrimary,
                            disabledContainerColor = AppColors.SurfaceOverlay,
                            disabledContentColor = AppColors.TextDisabled
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (canUndo) AppColors.TextPrimary else AppColors.TextDisabled
                        )
                        Text(
                            text = "撤销",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = AppPadding.SM),
                            color = if (canUndo) AppColors.TextPrimary else AppColors.TextDisabled
                        )
                    }
                }

                Button(
                    onClick = onConfirmDelete,
                    modifier = Modifier.weight(1f),
                    enabled = deleteQueue.items.isNotEmpty(),
                    shape = AppShape.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Destructive,
                        disabledContainerColor = AppColors.SurfaceOverlay
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (deleteQueue.items.isNotEmpty()) Color.White else AppColors.TextDisabled
                    )
                    Text(
                        text = "确认删除",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = AppPadding.SM),
                        color = if (deleteQueue.items.isNotEmpty()) Color.White else AppColors.TextDisabled
                    )
                }
            }
        }
    }
}
