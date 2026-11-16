package com.example.wallpaperapp.ui.zone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.repository.ImageRepository
import com.example.wallpaperapp.domain.repository.QuoteRepository
import com.example.wallpaperapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ZoneEditorViewModel(
    private val settingsRepository: SettingsRepository,
    private val quoteRepository: QuoteRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    val normalizedRect: StateFlow<NormalizedRect> = settingsRepository.observeNormalizedRect()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NormalizedRect.CENTER)

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

    fun applyPreset(rect: NormalizedRect) {
        updateRect(rect)
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
