package com.gallery.cleaner.domain.model

import androidx.compose.ui.graphics.Color

data class ThemeConfig(
    val primaryColor: Color = ThemePreset.OCEAN_BLUE.color,
    val presetName: String = ThemePreset.OCEAN_BLUE.name
)

enum class ThemePreset(val displayName: String, val color: Color) {
    OCEAN_BLUE("海洋蓝", Color(0xFF0A84FF)),
    EMERALD("翡翠绿", Color(0xFF30D158)),
    AMBER("琥珀橙", Color(0xFFFF9F0A)),
    ROSE("玫瑰红", Color(0xFFFF375F)),
    LAVENDER("薰衣紫", Color(0xFFBF5AF2)),
    CYAN("青碧", Color(0xFF64D2FF)),
    SUNSET("落日金", Color(0xFFFFD60A)),
    MINT("薄荷绿", Color(0xFF66D4CF));

    companion object {
        const val CUSTOM_PREFIX = "custom"
    }
}
