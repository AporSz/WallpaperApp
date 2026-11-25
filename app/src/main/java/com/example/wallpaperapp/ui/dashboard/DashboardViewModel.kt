package com.example.wallpaperapp.ui.dashboard

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.local.FontStorageManager
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.BackupScope
import com.example.wallpaperapp.domain.model.ScheduleConfig
import com.example.wallpaperapp.domain.model.TextColorScheme
import com.example.wallpaperapp.domain.model.WallpaperTarget
import com.example.wallpaperapp.domain.repository.LibraryBackupRepository
import com.example.wallpaperapp.domain.repository.SettingsRepository
import com.example.wallpaperapp.domain.scheduler.WallpaperScheduler
import com.example.wallpaperapp.domain.usecase.RotateWallpaperUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ApplyState {
    object Idle : ApplyState
    object Loading : ApplyState
    data class Success(val message: String) : ApplyState
    data class Error(val message: String) : ApplyState
}

class DashboardViewModel(
    private val settingsRepository: SettingsRepository,
    private val wallpaperScheduler: WallpaperScheduler,
    private val rotateWallpaperUseCase: RotateWallpaperUseCase,
    private val libraryBackupRepository: LibraryBackupRepository
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

    val textGlowEnabledState: StateFlow<Boolean> = settingsRepository.observeTextGlowEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _applyState = MutableStateFlow<ApplyState>(ApplyState.Idle)
    val applyState: StateFlow<ApplyState> = _applyState.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val _exportScopeState = MutableStateFlow(BackupScope.BOTH)
    val exportScopeState: StateFlow<BackupScope> = _exportScopeState.asStateFlow()

    private val _importScopeState = MutableStateFlow(BackupScope.BOTH)
    val importScopeState: StateFlow<BackupScope> = _importScopeState.asStateFlow()

    private val _isBackupInProgress = MutableStateFlow(false)
    val isBackupInProgress: StateFlow<Boolean> = _isBackupInProgress.asStateFlow()

    fun openExportDialog() { _showExportDialog.value = true }
    fun dismissExportDialog() { _showExportDialog.value = false }
    fun setExportScope(scope: BackupScope) { _exportScopeState.value = scope }

    fun openImportDialog() { _showImportDialog.value = true }
    fun dismissImportDialog() { _showImportDialog.value = false }
    fun setImportScope(scope: BackupScope) { _importScopeState.value = scope }

    fun exportLibrary(destinationUri: Uri) {
        viewModelScope.launch {
            _isBackupInProgress.value = true
            val scope = _exportScopeState.value
            val result = libraryBackupRepository.exportLibrary(destinationUri.toString(), scope)
            _isBackupInProgress.value = false
            if (result.isSuccess) {
                val summary = result.getOrNull()
                val msg = if (summary != null) {
                    var text = "Exported ${summary.exportedQuotesCount} quotes and ${summary.exportedImagesCount} images successfully!"
                    if (summary.skippedImagesCount > 0) {
                        text += " (${summary.skippedImagesCount} unreadable images skipped)"
                    }
                    text
                } else {
                    "Library backup exported successfully!"
                }
                _message.emit(msg)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Export failed"
                _message.emit("Export Error: $errorMsg")
            }
        }
    }

    fun importLibrary(sourceUri: Uri) {
        viewModelScope.launch {
            _isBackupInProgress.value = true
            val scope = _importScopeState.value
            val result = libraryBackupRepository.importLibrary(sourceUri.toString(), scope)
            _isBackupInProgress.value = false
            if (result.isSuccess) {
                val summary = result.getOrNull()
                val msg = if (summary != null) {
                    var text = "Imported ${summary.importedQuotesCount} quotes and ${summary.importedImagesCount} images!"
                    if (summary.skippedDuplicateQuotesCount > 0) {
                        text += " (${summary.skippedDuplicateQuotesCount} duplicate quotes skipped)"
                    }
                    text
                } else {
                    "Library imported successfully!"
                }
                _message.emit(msg)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Import failed"
                _message.emit("Import Error: $errorMsg")
            }
        }
    }

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

    fun setTextGlowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTextGlowEnabled(enabled)
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

    fun resetApplyState() {
        _applyState.value = ApplyState.Idle
    }

    fun rotateNow() {
        viewModelScope.launch {
            _applyState.value = ApplyState.Loading
            val result = rotateWallpaperUseCase()
            if (result.isSuccess) {
                val successMsg = "Wallpaper updated successfully!"
                _applyState.value = ApplyState.Success(successMsg)
                _message.emit(successMsg)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update wallpaper"
                _applyState.value = ApplyState.Error(errorMsg)
                _message.emit("Error: $errorMsg")
            }
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val wallpaperScheduler: WallpaperScheduler,
        private val rotateWallpaperUseCase: RotateWallpaperUseCase,
        private val libraryBackupRepository: LibraryBackupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                settingsRepository,
                wallpaperScheduler,
                rotateWallpaperUseCase,
                libraryBackupRepository
            ) as T
        }
    }
}
