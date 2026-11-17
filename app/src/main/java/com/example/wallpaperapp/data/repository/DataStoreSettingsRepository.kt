package com.example.wallpaperapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.ScheduleConfig
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.domain.model.WallpaperTarget
import com.example.wallpaperapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val WALLPAPER_TARGET = stringPreferencesKey("key_wallpaper_target")
        val RECT_LEFT = floatPreferencesKey("key_rect_left")
        val RECT_TOP = floatPreferencesKey("key_rect_top")
        val RECT_RIGHT = floatPreferencesKey("key_rect_right")
        val RECT_BOTTOM = floatPreferencesKey("key_rect_bottom")
        val SCHEDULE_ENABLED = booleanPreferencesKey("key_schedule_enabled")
        val SCHEDULE_INTERVAL = longPreferencesKey("key_schedule_interval")
        val QUOTE_SHUFFLE_QUEUE = stringPreferencesKey("key_quote_shuffle_queue")
        val IMAGE_SHUFFLE_QUEUE = stringPreferencesKey("key_image_shuffle_queue")
        val APP_THEME_MODE = stringPreferencesKey("key_app_theme_mode")
        val TEXT_ORIENTATION = stringPreferencesKey("key_text_orientation")
        val QUOTE_FONT_ID = stringPreferencesKey("key_quote_font_id")
        val IS_BOLD = booleanPreferencesKey("key_is_bold")
        val IS_ITALIC = booleanPreferencesKey("key_is_italic")
        val SHUFFLE_FONTS = booleanPreferencesKey("key_shuffle_fonts")
        val FONT_SHUFFLE_QUEUE = stringPreferencesKey("key_font_shuffle_queue")
    }

    override fun observeWallpaperTarget(): Flow<WallpaperTarget> {
        return dataStore.data.map { prefs ->
            val name = prefs[PreferencesKeys.WALLPAPER_TARGET] ?: WallpaperTarget.BOTH.name
            try {
                WallpaperTarget.valueOf(name)
            } catch (e: Exception) {
                WallpaperTarget.BOTH
            }
        }
    }

    override suspend fun setWallpaperTarget(target: WallpaperTarget) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.WALLPAPER_TARGET] = target.name
        }
    }

    override fun observeNormalizedRect(): Flow<NormalizedRect> {
        return dataStore.data.map { prefs ->
            val left = prefs[PreferencesKeys.RECT_LEFT] ?: NormalizedRect.CENTER.left
            val top = prefs[PreferencesKeys.RECT_TOP] ?: NormalizedRect.CENTER.top
            val right = prefs[PreferencesKeys.RECT_RIGHT] ?: NormalizedRect.CENTER.right
            val bottom = prefs[PreferencesKeys.RECT_BOTTOM] ?: NormalizedRect.CENTER.bottom
            try {
                NormalizedRect(left, top, right, bottom)
            } catch (e: Exception) {
                NormalizedRect.CENTER
            }
        }
    }

    override suspend fun setNormalizedRect(rect: NormalizedRect) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.RECT_LEFT] = rect.left
            prefs[PreferencesKeys.RECT_TOP] = rect.top
            prefs[PreferencesKeys.RECT_RIGHT] = rect.right
            prefs[PreferencesKeys.RECT_BOTTOM] = rect.bottom
        }
    }

    override fun observeScheduleConfig(): Flow<ScheduleConfig> {
        return dataStore.data.map { prefs ->
            val enabled = prefs[PreferencesKeys.SCHEDULE_ENABLED] ?: false
            val interval = prefs[PreferencesKeys.SCHEDULE_INTERVAL] ?: 60L
            ScheduleConfig(isEnabled = enabled, intervalMinutes = interval)
        }
    }

    override suspend fun setScheduleConfig(config: ScheduleConfig) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SCHEDULE_ENABLED] = config.isEnabled
            prefs[PreferencesKeys.SCHEDULE_INTERVAL] = config.intervalMinutes
        }
    }

    override fun observeQuoteShuffleQueue(): Flow<List<String>> {
        return dataStore.data.map { prefs ->
            val raw = prefs[PreferencesKeys.QUOTE_SHUFFLE_QUEUE] ?: ""
            parseQueue(raw)
        }
    }

    override suspend fun setQuoteShuffleQueue(queue: List<String>) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.QUOTE_SHUFFLE_QUEUE] = formatQueue(queue)
        }
    }

    override fun observeImageShuffleQueue(): Flow<List<String>> {
        return dataStore.data.map { prefs ->
            val raw = prefs[PreferencesKeys.IMAGE_SHUFFLE_QUEUE] ?: ""
            parseQueue(raw)
        }
    }

    override suspend fun setImageShuffleQueue(queue: List<String>) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.IMAGE_SHUFFLE_QUEUE] = formatQueue(queue)
        }
    }

    override fun observeAppThemeMode(): Flow<AppThemeMode> {
        return dataStore.data.map { prefs ->
            val name = prefs[PreferencesKeys.APP_THEME_MODE] ?: AppThemeMode.SYSTEM.name
            try {
                AppThemeMode.valueOf(name)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }
        }
    }

    override suspend fun setAppThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_THEME_MODE] = themeMode.name
        }
    }

    override fun observeTextOrientation(): Flow<TextOrientation> {
        return dataStore.data.map { prefs ->
            val name = prefs[PreferencesKeys.TEXT_ORIENTATION] ?: TextOrientation.HORIZONTAL.name
            try {
                TextOrientation.valueOf(name)
            } catch (e: Exception) {
                TextOrientation.HORIZONTAL
            }
        }
    }

    override suspend fun setTextOrientation(orientation: TextOrientation) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TEXT_ORIENTATION] = orientation.name
        }
    }

    override fun observeQuoteFontId(): Flow<String> {
        return dataStore.data.map { prefs ->
            prefs[PreferencesKeys.QUOTE_FONT_ID] ?: "builtin_sans"
        }
    }

    override suspend fun setQuoteFontId(fontId: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.QUOTE_FONT_ID] = fontId
        }
    }

    override fun observeIsBold(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[PreferencesKeys.IS_BOLD] ?: false
        }
    }

    override suspend fun setIsBold(isBold: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_BOLD] = isBold
        }
    }

    override fun observeIsItalic(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[PreferencesKeys.IS_ITALIC] ?: false
        }
    }

    override suspend fun setIsItalic(isItalic: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_ITALIC] = isItalic
        }
    }

    override fun observeShuffleFonts(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[PreferencesKeys.SHUFFLE_FONTS] ?: false
        }
    }

    override suspend fun setShuffleFonts(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHUFFLE_FONTS] = enabled
        }
    }

    override fun observeFontShuffleQueue(): Flow<List<String>> {
        return dataStore.data.map { prefs ->
            val raw = prefs[PreferencesKeys.FONT_SHUFFLE_QUEUE] ?: ""
            parseQueue(raw)
        }
    }

    override suspend fun setFontShuffleQueue(queue: List<String>) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FONT_SHUFFLE_QUEUE] = formatQueue(queue)
        }
    }

    private fun parseQueue(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
    }

    private fun formatQueue(queue: List<String>): String {
        return queue.joinToString(",")
    }
}
