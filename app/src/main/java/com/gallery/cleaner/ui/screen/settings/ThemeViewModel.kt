package com.gallery.cleaner.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.domain.model.ThemeConfig
import com.gallery.cleaner.domain.model.ThemePreset
import com.gallery.cleaner.domain.repository.ThemeRepository
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ThemeVM"
    }

    val themeConfig = themeRepository.getThemeConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeConfig())

    fun setPreset(preset: ThemePreset) {
        AppLogger.userAction(TAG, "setPreset", "preset=${preset.name}")
        viewModelScope.launch {
            themeRepository.setPreset(preset)
        }
    }

    fun setCustomColor(color: androidx.compose.ui.graphics.Color) {
        AppLogger.userAction(TAG, "setCustomColor", "color=$color")
        viewModelScope.launch {
            themeRepository.setCustomColor(color)
        }
    }
}
