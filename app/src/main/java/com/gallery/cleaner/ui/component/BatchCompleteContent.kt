package com.gallery.cleaner.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallery.cleaner.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun BatchCompleteContent(
    batchTotal: Int,
    keptCount: Int,
    queuedCount: Int,
    onConfirmDelete: (() -> Unit)? = null,
    onNextBatch: (() -> Unit)? = null,
    onExit: () -> Unit
) {
    var showStats by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    val animatedTotal = animateCount(batchTotal)
    val animatedKept = animateCount(keptCount)
    val animatedQueued = animateCount(queuedCount)

    LaunchedEffect(Unit) {
        delay(100)
        showStats = true
        delay(200)
        showButtons = true
    }

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
                    text = "SWIPE SEQUENCE COMPLETE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "本轮清理完成",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Black
                )

                AnimatedVisibility(visible = showStats) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppPadding.SM)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppPadding.LG),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatBadge(label = "总数", value = "${animatedTotal.value.toInt()}")
                            StatBadge(label = "保留", value = "${animatedKept.value.toInt()}", color = AppColors.Success)
                            if (queuedCount > 0) {
                                StatBadge(label = "删除", value = "${animatedQueued.value.toInt()}", color = AppColors.Destructive)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppPadding.SM))

                AnimatedVisibility(
                    visible = showButtons,
                    enter = Motion.Enter.slideInFromBottom()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppPadding.SM)
                    ) {
                        if (queuedCount > 0 && onConfirmDelete != null) {
                            Button(
                                onClick = onConfirmDelete,
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShape.Medium,
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Destructive),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("确认删除 ($queuedCount 张)", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if (onNextBatch != null) {
                            Button(
                                onClick = onNextBatch,
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShape.Medium,
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("下一批", fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = onExit,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("返回首页", color = AppColors.TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    color: Color = AppColors.Primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
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

@Composable
private fun animateCount(target: Int): Animatable<Float, *> {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 600)
        )
    }
    return animatable
}
