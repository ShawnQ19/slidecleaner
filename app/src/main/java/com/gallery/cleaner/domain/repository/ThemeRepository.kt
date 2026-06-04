package com.gallery.cleaner.domain.repository

import androidx.compose.ui.graphics.Color
import com.gallery.cleaner.domain.model.ThemeConfig
import com.gallery.cleaner.domain.model.ThemePreset
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemeConfig(): Flow<ThemeConfig>
    suspend fun setThemeConfig(config: ThemeConfig)
    suspend fun setPreset(preset: ThemePreset)
    suspend fun setCustomColor(color: Color)
}
