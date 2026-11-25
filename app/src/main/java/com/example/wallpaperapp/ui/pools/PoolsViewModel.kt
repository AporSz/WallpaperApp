package com.example.wallpaperapp.ui.pools

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.domain.model.LandscapeHandlingMode
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.TextColorScheme
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.domain.model.WallpaperImage
import com.example.wallpaperapp.domain.repository.ImageRepository
import com.example.wallpaperapp.domain.repository.QuoteRepository
import com.example.wallpaperapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PoolsViewModel(
    private val quoteRepository: QuoteRepository,
    private val imageRepository: ImageRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val quotesState: StateFlow<List<Quote>> = quoteRepository.observeAllQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val imagesState: StateFlow<List<WallpaperImage>> = imageRepository.observeAllImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val landscapeHandlingModeState: StateFlow<LandscapeHandlingMode> = settingsRepository.observeLandscapeHandlingMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LandscapeHandlingMode.ROTATE_90_CW)

    val rectState: StateFlow<NormalizedRect> = settingsRepository.observeNormalizedRect()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NormalizedRect.CENTER)

    val orientationState: StateFlow<TextOrientation> = settingsRepository.observeTextOrientation()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TextOrientation.HORIZONTAL)

    val fontIdState: StateFlow<String> = settingsRepository.observeQuoteFontId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "builtin_sans")

    val isBoldState: StateFlow<Boolean> = settingsRepository.observeIsBold()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isItalicState: StateFlow<Boolean> = settingsRepository.observeIsItalic()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val textColorSchemeState: StateFlow<TextColorScheme> = settingsRepository.observeTextColorScheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TextColorScheme.HIGH_CONTRAST)

    val textGlowEnabledState: StateFlow<Boolean> = settingsRepository.observeTextGlowEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun setLandscapeHandlingMode(mode: LandscapeHandlingMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setLandscapeHandlingMode(mode)
            } catch (e: Exception) {
                Log.e("PoolsViewModel", "Failed to update landscape handling mode", e)
                _message.emit("Failed to update settings: ${e.message}")
            }
        }
    }

    fun addQuote(text: String, author: String?, showQuotationMarks: Boolean = true) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val quote = Quote(
                    id = UUID.randomUUID().toString(),
                    text = text.trim(),
                    author = author?.trim()?.ifBlank { null },
                    showQuotationMarks = showQuotationMarks
                )
                quoteRepository.addQuote(quote)
            } catch (e: Exception) {
                Log.e("PoolsViewModel", "Failed to add quote", e)
                _message.emit("Failed to add quote: ${e.message}")
            }
        }
    }

    fun updateQuote(id: String, text: String, author: String?, showQuotationMarks: Boolean) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val quote = Quote(
                    id = id,
                    text = text.trim(),
                    author = author?.trim()?.ifBlank { null },
                    showQuotationMarks = showQuotationMarks
                )
                quoteRepository.updateQuote(quote)
            } catch (e: Exception) {
                Log.e("PoolsViewModel", "Failed to update quote", e)
                _message.emit("Failed to update quote: ${e.message}")
            }
        }
    }

    fun deleteQuote(id: String) {
        viewModelScope.launch {
            try {
                quoteRepository.deleteQuote(id)
            } catch (e: Exception) {
                Log.e("PoolsViewModel", "Failed to delete quote", e)
                _message.emit("Failed to delete quote: ${e.message}")
            }
        }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            try {
                val newImages = uris.map { uri ->
                    WallpaperImage(
                        id = UUID.randomUUID().toString(),
                        uri = uri.toString()
                    )
                }
                imageRepository.addImages(newImages)
            } catch (e: Exception) {
                Log.e("PoolsViewModel", "Failed to add images", e)
                _message.emit("Failed to add images: ${e.message}")
            }
        }
    }

    fun deleteImage(id: String) {
        viewModelScope.launch {
            try {
                imageRepository.deleteImage(id)
            } catch (e: Exception) {
                Log.e("PoolsViewModel", "Failed to delete image", e)
                _message.emit("Failed to delete image: ${e.message}")
            }
        }
    }

    class Factory(
        private val quoteRepository: QuoteRepository,
        private val imageRepository: ImageRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PoolsViewModel(quoteRepository, imageRepository, settingsRepository) as T
        }
    }
}
