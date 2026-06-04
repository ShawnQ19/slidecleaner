package com.gallery.cleaner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.gallery.cleaner.ui.screen.settings.ThemeViewModel

private fun buildDarkScheme(colors: AppColorScheme) = darkColorScheme(
    primary = colors.primary,
    onPrimary = Color.White,
    primaryContainer = colors.primaryVariant,
    onPrimaryContainer = colors.textPrimary,
    secondary = colors.accent,
    onSecondary = Color.White,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surfaceElevated,
    onSurfaceVariant = colors.textSecondary,
    error = colors.destructive,
    onError = Color.White,
    outline = colors.separatorOpaque,
    outlineVariant = colors.separator,
    inverseSurface = Color(0xFFF5F8FF),
    inverseOnSurface = Color(0xFF07111E),
    surfaceTint = colors.primary,
    scrim = Color(0xCC000000)
)

@Composable
fun GalleryCleanerTheme(
    viewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val themeConfig by viewModel.themeConfig.collectAsState()
    val colorScheme = appColorScheme(themeConfig.primaryColor)
    val darkScheme = buildDarkScheme(colorScheme)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppColorScheme provides colorScheme
    ) {
        MaterialTheme(
            colorScheme = darkScheme,
            typography = Typography,
            content = content
        )
    }
}
