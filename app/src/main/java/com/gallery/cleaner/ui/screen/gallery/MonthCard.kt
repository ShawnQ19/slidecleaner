package com.gallery.cleaner.ui.screen.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallery.cleaner.domain.model.MediaGroup
import com.gallery.cleaner.ui.component.AppPadding
import com.gallery.cleaner.ui.component.AppShape
import com.gallery.cleaner.ui.component.GlassCard
import com.gallery.cleaner.ui.component.MediaThumbnail
import com.gallery.cleaner.ui.component.Responsive
import com.gallery.cleaner.ui.component.pressClick
import com.gallery.cleaner.ui.theme.AppColors

@Composable
fun MonthCard(
    group: MediaGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewItems = group.mediaItems.take(Responsive.monthCardPreviewCount)
    val columns = Responsive.gridColumns
    val thumbnailSize = 240

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .pressClick(onClick = onClick),
        shape = AppShape.Large,
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppPadding.LG, vertical = AppPadding.MD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${group.mediaItems.size} 个项目",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
            Text(
                text = "\u276F",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Primary
            )
        }

        if (previewItems.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppPadding.MD)
                    .padding(bottom = AppPadding.MD),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                previewItems.chunked(columns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(AppShape.Small)
                            ) {
                                MediaThumbnail(
                                    mediaItem = item,
                                    modifier = Modifier.fillMaxWidth(),
                                    size = thumbnailSize
                                )
                            }
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                val remaining = group.mediaItems.size - previewItems.size
                if (remaining > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShape.Small)
                            .background(AppColors.SurfaceOverlay)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
