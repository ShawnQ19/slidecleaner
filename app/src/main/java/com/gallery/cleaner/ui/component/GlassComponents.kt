package com.gallery.cleaner.ui.component

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.gallery.cleaner.ui.theme.AppColors
import com.gallery.cleaner.ui.theme.GlassColors
import kotlinx.coroutines.launch

object AppShape {
    val Small = RoundedCornerShape(14.dp)
    val Medium = RoundedCornerShape(18.dp)
    val Large = RoundedCornerShape(24.dp)
    val XLarge = RoundedCornerShape(32.dp)
    val Pill = RoundedCornerShape(50)
    val Circle = RoundedCornerShape(50)
}

object AppPadding {
    val XS = 4.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 18.dp
    val XL = 24.dp
    val XXL = 28.dp
}

@Composable
fun Modifier.glassBlur(blurRadius: Float = 25f): Modifier {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val radiusPx = with(density) { blurRadius.dp.toPx() }
    val blurEffect = remember(radiusPx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) BlurEffect(radiusPx, radiusPx) else null
    }
    return if (blurEffect != null) {
        this.graphicsLayer { renderEffect = blurEffect }
    } else {
        this
    }
}

@Composable
fun Modifier.pressClick(
    onClick: () -> Unit
): Modifier {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                scope.launch {
                    scale.animateTo(0.96f, spring(stiffness = Spring.StiffnessHigh))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                }
                onClick()
            }
        )
}

@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val separatorColor = AppColors.Separator
    Box(
        modifier = modifier
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassColors.TopBarBackground,
                            GlassColors.TopBarBackgroundEnd
                        )
                    )
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GlassColors.HighlightLine,
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = separatorColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            },
        content = content
    )
}

@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val separatorColor = AppColors.Separator
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassColors.BottomBarBackground,
                            GlassColors.BottomBarBackgroundEnd
                        )
                    )
                )
                drawLine(
                    color = separatorColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .padding(horizontal = AppPadding.LG, vertical = AppPadding.MD),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AppShape.Medium,
    contentPadding: Dp = AppPadding.LG,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF101829),
                            Color(0xFF0B1220)
                        )
                    )
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GlassColors.HighlightLine,
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(1.dp, GlassColors.Border, shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassOverlay(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AppShape.Large,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassColors.DialogBackground,
                            Color(0xFF101A2D)
                        )
                    )
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GlassColors.BorderHighlight,
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(1.dp, GlassColors.BorderHighlight, shape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassSwipeHint(
    modifier: Modifier = Modifier,
    progress: Float,
    title: String = "上滑加入删除队列",
    subtitle: String = "松开后即可加入队列 · 可撤销"
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    if (clampedProgress > 0f) {
        val detailAlpha = ((clampedProgress - 0.42f) / 0.34f).coerceIn(0f, 1f)
        val containerAlpha = (0.58f + clampedProgress * 0.42f).coerceIn(0.58f, 1f)

        Box(
            modifier = modifier
                .clip(AppShape.Pill)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD0161F2E),
                            Color(0xBF0C1421)
                        )
                    )
                )
                .border(1.dp, Color(0x33FFD58A), AppShape.Pill)
                .padding(horizontal = AppPadding.MD, vertical = AppPadding.XS)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppPadding.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(AppShape.Pill)
                            .background(Color(0x1AFFD58A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↑",
                            color = Color(0xFFFFD58A),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color(0xFFFFE8BE).copy(alpha = containerAlpha),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (detailAlpha > 0f) {
                            Text(
                                text = subtitle,
                                color = Color.White.copy(alpha = 0.48f + detailAlpha * 0.30f),
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(AppShape.Pill)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(clampedProgress.coerceAtLeast(0.08f))
                            .height(2.dp)
                            .clip(AppShape.Pill)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFD58A),
                                        Color(0xFFFF9F43)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
