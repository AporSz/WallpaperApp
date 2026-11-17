package com.example.wallpaperapp.ui.zone

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.local.FontStorageManager
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.domain.repository.ImageRepository
import com.example.wallpaperapp.domain.repository.QuoteRepository
import com.example.wallpaperapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ZoneEditorViewModel(
    private val settingsRepository: SettingsRepository,
    private val quoteRepository: QuoteRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    val normalizedRect: StateFlow<NormalizedRect> = settingsRepository.observeNormalizedRect()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NormalizedRect.CENTER)

    val textOrientation: StateFlow<TextOrientation> = settingsRepository.observeTextOrientation()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TextOrientation.HORIZONTAL)

    val quoteFontId: StateFlow<String> = settingsRepository.observeQuoteFontId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "builtin_sans")

    val isBoldState: StateFlow<Boolean> = settingsRepository.observeIsBold()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isItalicState: StateFlow<Boolean> = settingsRepository.observeIsItalic()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _sampleQuote = MutableStateFlow<Quote?>(null)
    val sampleQuote: StateFlow<Quote?> = _sampleQuote.asStateFlow()

    private val _sampleImageUri = MutableStateFlow<String?>(null)
    val sampleImageUri: StateFlow<String?> = _sampleImageUri.asStateFlow()

    init {
        loadSamples()
    }

    private fun loadSamples() {
        viewModelScope.launch {
            val quotes = quoteRepository.getAllQuotes()
            if (quotes.isNotEmpty()) {
                _sampleQuote.value = quotes.first()
            } else {
                _sampleQuote.value = Quote("default", "The journey of a thousand miles begins with a single step.", "Lao Tzu")
            }

            val images = imageRepository.getAllImages()
            if (images.isNotEmpty()) {
                _sampleImageUri.value = images.first().uri
            }
        }
    }

    fun updateRect(rect: NormalizedRect) {
        viewModelScope.launch {
            settingsRepository.setNormalizedRect(rect)
        }
    }

    fun setTextOrientation(orientation: TextOrientation) {
        viewModelScope.launch {
            settingsRepository.setTextOrientation(orientation)
        }
    }

    fun setQuoteFontId(fontId: String) {
        viewModelScope.launch {
            settingsRepository.setQuoteFontId(fontId)
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

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val quoteRepository: QuoteRepository,
        private val imageRepository: ImageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ZoneEditorViewModel(settingsRepository, quoteRepository, imageRepository) as T
        }
    }
}
