package com.gallery.cleaner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColorScheme(
    val primary: Color,
    val primaryVariant: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceOverlay: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val destructive: Color,
    val destructiveDim: Color,
    val success: Color,
    val warning: Color,
    val separator: Color,
    val separatorOpaque: Color,
    val ripple: Color
)

fun appColorScheme(primary: Color): AppColorScheme {
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(
        (primary.alpha * 255).toInt().shl(24) or
            (primary.red * 255).toInt().shl(16) or
            (primary.green * 255).toInt().shl(8) or
            (primary.blue * 255).toInt(),
        hsl
    )
    val hue = hsl[0]
    val sat = hsl[1]
    val light = hsl[2]

    val primaryVariant = Color(
        android.graphics.Color.HSVToColor(floatArrayOf(hue, (sat * 1.1f).coerceAtMost(1f), (light * 0.85f).coerceAtMost(1f)))
    )
    val accent = Color(
        android.graphics.Color.HSVToColor(floatArrayOf((hue + 28) % 360, (sat * 0.8f).coerceAtMost(1f), (light + 0.16f).coerceAtMost(1f)))
    )

    return AppColorScheme(
        primary = primary,
        primaryVariant = primaryVariant.copy(alpha = 0.88f),
        accent = accent,
        background = Color(0xFF07111E),
        surface = Color(0xFF0D1626),
        surfaceElevated = Color(0xFF131C2D),
        surfaceOverlay = Color(0xFF1A2540),
        textPrimary = Color(0xFFF5F8FF),
        textSecondary = Color(0xB8D4E2FF),
        textTertiary = Color(0x84D4E2FF),
        textDisabled = Color(0x44D4E2FF),
        destructive = Color(0xFFFF6C7C),
        destructiveDim = Color(0x22FF6C7C),
        success = Color(0xFF42D8A0),
        warning = Color(0xFFFFC85C),
        separator = Color(0x226A88B7),
        separatorOpaque = Color(0xFF27344A),
        ripple = Color(0x2678A8FF)
    )
}

val LocalAppColorScheme = staticCompositionLocalOf { appColorScheme(Color(0xFF0A84FF)) }

object GlassColors {
    val TopBarBackground = Color(0xCC0B1220)
    val TopBarBackgroundEnd = Color(0xE6101A2D)
    val BottomBarBackground = Color(0xE60B1220)
    val BottomBarBackgroundEnd = Color(0xCC101A2D)
    val CardBackground = Color(0xF2101726)
    val DialogBackground = Color(0xFF0D1626)
    val Border = Color(0x296A88B7)
    val BorderHighlight = Color(0x3D7CB6FF)
    val HighlightLine = Color(0x1EFFFFFF)
    val InnerShadow = Color(0x12000000)
}

object AppColors {
    val Primary @Composable get() = LocalAppColorScheme.current.primary
    val PrimaryVariant @Composable get() = LocalAppColorScheme.current.primaryVariant
    val Accent @Composable get() = LocalAppColorScheme.current.accent
    val Background @Composable get() = LocalAppColorScheme.current.background
    val Surface @Composable get() = LocalAppColorScheme.current.surface
    val SurfaceElevated @Composable get() = LocalAppColorScheme.current.surfaceElevated
    val SurfaceOverlay @Composable get() = LocalAppColorScheme.current.surfaceOverlay
    val TextPrimary @Composable get() = LocalAppColorScheme.current.textPrimary
    val TextSecondary @Composable get() = LocalAppColorScheme.current.textSecondary
    val TextTertiary @Composable get() = LocalAppColorScheme.current.textTertiary
    val TextDisabled @Composable get() = LocalAppColorScheme.current.textDisabled
    val Destructive @Composable get() = LocalAppColorScheme.current.destructive
    val DestructiveDim @Composable get() = LocalAppColorScheme.current.destructiveDim
    val Success @Composable get() = LocalAppColorScheme.current.success
    val Warning @Composable get() = LocalAppColorScheme.current.warning
    val Separator @Composable get() = LocalAppColorScheme.current.separator
    val SeparatorOpaque @Composable get() = LocalAppColorScheme.current.separatorOpaque
    val Ripple @Composable get() = LocalAppColorScheme.current.ripple
}
