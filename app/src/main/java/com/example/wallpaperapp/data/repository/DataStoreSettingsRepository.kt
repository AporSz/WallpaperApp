package com.example.wallpaperapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.ScheduleConfig
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

    private fun parseQueue(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
    }

    private fun formatQueue(queue: List<String>): String {
        return queue.joinToString(",")
    }
}
