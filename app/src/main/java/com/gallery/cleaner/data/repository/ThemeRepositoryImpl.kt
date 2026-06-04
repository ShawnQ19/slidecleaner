package com.gallery.cleaner.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gallery.cleaner.domain.model.ThemeConfig
import com.gallery.cleaner.domain.model.ThemePreset
import com.gallery.cleaner.domain.repository.ThemeRepository
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    companion object {
        private const val TAG = "ThemeRepo"
        private val KEY_PRIMARY_COLOR = longPreferencesKey("primary_color")
        private val KEY_PRESET_NAME = stringPreferencesKey("preset_name")
    }

    override fun getThemeConfig(): Flow<ThemeConfig> {
        return context.themeDataStore.data.map { prefs ->
            val colorValue = prefs[KEY_PRIMARY_COLOR]
            val presetName = prefs[KEY_PRESET_NAME]
            if (colorValue != null) {
                val preset = ThemePreset.entries.find { it.name == presetName }
                ThemeConfig(
                    primaryColor = Color(colorValue),
                    presetName = preset?.name ?: ThemePreset.CUSTOM_PREFIX
                )
            } else {
                ThemeConfig()
            }
        }
    }

    override suspend fun setThemeConfig(config: ThemeConfig) {
        AppLogger.userAction(TAG, "setThemeConfig", "color=${config.primaryColor}, preset=${config.presetName}")
        context.themeDataStore.edit { prefs ->
            prefs[KEY_PRIMARY_COLOR] = config.primaryColor.toArgb().toLong()
            prefs[KEY_PRESET_NAME] = config.presetName
        }
    }

    override suspend fun setPreset(preset: ThemePreset) {
        AppLogger.userAction(TAG, "setPreset", "preset=${preset.name}")
        context.themeDataStore.edit { prefs ->
            prefs[KEY_PRIMARY_COLOR] = preset.color.toArgb().toLong()
            prefs[KEY_PRESET_NAME] = preset.name
        }
    }

    override suspend fun setCustomColor(color: Color) {
        AppLogger.userAction(TAG, "setCustomColor", "color=$color")
        context.themeDataStore.edit { prefs ->
            prefs[KEY_PRIMARY_COLOR] = color.toArgb().toLong()
            prefs[KEY_PRESET_NAME] = ThemePreset.CUSTOM_PREFIX
        }
    }
}
