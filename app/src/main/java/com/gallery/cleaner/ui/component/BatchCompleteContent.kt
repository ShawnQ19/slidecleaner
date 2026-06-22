package com.gallery.cleaner.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallery.cleaner.ui.theme.AppColors

@Composable
fun BatchCompleteContent(
    batchTotal: Int,
    keptCount: Int,
    queuedCount: Int,
    onConfirmDelete: (() -> Unit)? = null,
    onNextBatch: (() -> Unit)? = null,
    onExit: () -> Unit
) {
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "共 $batchTotal 张，保留 $keptCount 张" + if (queuedCount > 0) "，删除队列 $queuedCount 张" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                if (queuedCount > 0 && onConfirmDelete != null) {
                    Button(
                        onClick = onConfirmDelete,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShape.Medium,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Destructive),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("确认删除 ($queuedCount 张)", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
                if (onNextBatch != null) {
                    Button(
                        onClick = onNextBatch,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShape.Medium,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("下一批", fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("返回首页", color = AppColors.TextSecondary)
                }
            }
        }
    }
}
