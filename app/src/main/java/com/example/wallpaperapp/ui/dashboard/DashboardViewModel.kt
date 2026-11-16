package com.example.wallpaperapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.ScheduleConfig
import com.example.wallpaperapp.domain.model.WallpaperTarget
import com.example.wallpaperapp.domain.repository.SettingsRepository
import com.example.wallpaperapp.domain.scheduler.WallpaperScheduler
import com.example.wallpaperapp.domain.usecase.RotateWallpaperUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
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
