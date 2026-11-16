package com.example.wallpaperapp.domain.repository

import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.ScheduleConfig
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.domain.model.WallpaperTarget
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeWallpaperTarget(): Flow<WallpaperTarget>
    suspend fun setWallpaperTarget(target: WallpaperTarget)

    fun observeNormalizedRect(): Flow<NormalizedRect>
    suspend fun setNormalizedRect(rect: NormalizedRect)

    fun observeScheduleConfig(): Flow<ScheduleConfig>
    suspend fun setScheduleConfig(config: ScheduleConfig)

    fun observeQuoteShuffleQueue(): Flow<List<String>>
    suspend fun setQuoteShuffleQueue(queue: List<String>)

    fun observeImageShuffleQueue(): Flow<List<String>>
    suspend fun setImageShuffleQueue(queue: List<String>)

    fun observeAppThemeMode(): Flow<AppThemeMode>
    suspend fun setAppThemeMode(themeMode: AppThemeMode)

    fun observeTextOrientation(): Flow<TextOrientation>
    suspend fun setTextOrientation(orientation: TextOrientation)
}
