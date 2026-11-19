package com.example.wallpaperapp.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.local.FontStorageManager
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.ScheduleConfig
import com.example.wallpaperapp.domain.model.TextColorScheme
import com.example.wallpaperapp.domain.model.WallpaperTarget
import com.example.wallpaperapp.domain.repository.SettingsRepository
import com.example.wallpaperapp.domain.scheduler.WallpaperScheduler
import com.example.wallpaperapp.domain.usecase.RotateWallpaperUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val settingsRepository: SettingsRepository,
    private val wallpaperScheduler: WallpaperScheduler,
    private val rotateWallpaperUseCase: RotateWallpaperUseCase
) : ViewModel() {

    val targetState: StateFlow<WallpaperTarget> = settingsRepository.observeWallpaperTarget()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WallpaperTarget.BOTH)

    val scheduleState: StateFlow<ScheduleConfig> = settingsRepository.observeScheduleConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleConfig())

    val themeModeState: StateFlow<AppThemeMode> = settingsRepository.observeAppThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    val quoteFontId: StateFlow<String> = settingsRepository.observeQuoteFontId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "builtin_sans")

    val isBoldState: StateFlow<Boolean> = settingsRepository.observeIsBold()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isItalicState: StateFlow<Boolean> = settingsRepository.observeIsItalic()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shuffleFontsState: StateFlow<Boolean> = settingsRepository.observeShuffleFonts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val textColorSchemeState: StateFlow<TextColorScheme> = settingsRepository.observeTextColorScheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TextColorScheme.HIGH_CONTRAST)

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun setWallpaperTarget(target: WallpaperTarget) {
        viewModelScope.launch {
            settingsRepository.setWallpaperTarget(target)
        }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = scheduleState.value
            val updated = current.copy(isEnabled = enabled)
            settingsRepository.setScheduleConfig(updated)
            wallpaperScheduler.schedule(updated)
        }
    }

    fun setScheduleInterval(intervalMinutes: Long) {
        viewModelScope.launch {
            val current = scheduleState.value
            val updated = current.copy(intervalMinutes = intervalMinutes)
            settingsRepository.setScheduleConfig(updated)
            wallpaperScheduler.schedule(updated)
        }
    }

    fun setAppThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.setAppThemeMode(themeMode)
        }
    }

    fun setQuoteFontId(fontId: String) {
        viewModelScope.launch {
            settingsRepository.setQuoteFontId(fontId)
        }
    }

    fun setIsBold(isBold: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIsBold(isBold)
        }
    }

    fun setIsItalic(isItalic: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIsItalic(isItalic)
        }
    }

    fun setShuffleFonts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShuffleFonts(enabled)
        }
    }

    fun setTextColorScheme(scheme: TextColorScheme) {
        viewModelScope.launch {
            settingsRepository.setTextColorScheme(scheme)
        }
    }

    fun importCustomFont(context: Context, uri: Uri) {
        viewModelScope.launch {
            val fontManager = FontStorageManager(context)
            val customPath = fontManager.saveCustomFont(uri)
            if (customPath != null) {
                val fontId = "custom_$customPath"
                settingsRepository.setQuoteFontId(fontId)
            }
        }
    }

    fun rotateNow() {
        viewModelScope.launch {
            val result = rotateWallpaperUseCase()
            if (result.isSuccess) {
                _message.emit("Wallpaper updated successfully!")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update wallpaper"
                _message.emit("Error: $errorMsg")
            }
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val wallpaperScheduler: WallpaperScheduler,
        private val rotateWallpaperUseCase: RotateWallpaperUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(settingsRepository, wallpaperScheduler, rotateWallpaperUseCase) as T
        }
    }
}
