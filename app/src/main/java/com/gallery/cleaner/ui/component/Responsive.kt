package com.gallery.cleaner.ui.component

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ScreenSize {
    Compact,
    Medium,
    Expanded
}

object Responsive {

    val screenSize: ScreenSize
        @Composable
        @ReadOnlyComposable
        get() {
            val config = LocalConfiguration.current
            val widthDp = config.screenWidthDp
            return when {
                widthDp < 360 -> ScreenSize.Compact
                widthDp < 600 -> ScreenSize.Medium
                else -> ScreenSize.Expanded
            }
        }

    val gridColumns: Int
        @Composable
        @ReadOnlyComposable
        get() = when (screenSize) {
            ScreenSize.Compact -> 3
            ScreenSize.Medium -> 4
            ScreenSize.Expanded -> 5
        }

    val gridContentPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = when (screenSize) {
            ScreenSize.Compact -> PaddingValues(8.dp)
            ScreenSize.Medium -> PaddingValues(12.dp)
            ScreenSize.Expanded -> PaddingValues(16.dp)
        }

    val gridSpacing: Dp
        @Composable
        @ReadOnlyComposable
        get() = when (screenSize) {
            ScreenSize.Compact -> 6.dp
            ScreenSize.Medium -> 8.dp
            ScreenSize.Expanded -> 10.dp
        }

    val listContentPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = when (screenSize) {
            ScreenSize.Compact -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ScreenSize.Medium -> PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ScreenSize.Expanded -> PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        }

    val listSpacing: Dp
        @Composable
        @ReadOnlyComposable
        get() = when (screenSize) {
            ScreenSize.Compact -> 12.dp
            ScreenSize.Medium -> 16.dp
            ScreenSize.Expanded -> 20.dp
        }

    val monthCardPreviewCount: Int
        @Composable
        @ReadOnlyComposable
        get() = when (screenSize) {
            ScreenSize.Compact -> 4
            ScreenSize.Medium -> 6
            ScreenSize.Expanded -> 9
        }

    val dialogMaxWidth: Dp
        @Composable
        @ReadOnlyComposable
        get() = when (screenSize) {
            ScreenSize.Compact -> 280.dp
            ScreenSize.Medium -> 320.dp
            ScreenSize.Expanded -> 360.dp
        }

    val bottomBarPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = when (screenSize) {
            ScreenSize.Compact -> PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ScreenSize.Medium -> PaddingValues(horizontal = 18.dp, vertical = 14.dp)
            ScreenSize.Expanded -> PaddingValues(horizontal = 24.dp, vertical = 18.dp)
        }

    @Composable
    fun gridCells(): GridCells {
        return GridCells.Fixed(gridColumns)
    }
}
