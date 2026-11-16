package com.example.wallpaperapp.ui.pools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.WallpaperImage
import com.example.wallpaperapp.domain.repository.ImageRepository
import com.example.wallpaperapp.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PoolsViewModel(
    private val quoteRepository: QuoteRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    val quotesState: StateFlow<List<Quote>> = quoteRepository.observeAllQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val imagesState: StateFlow<List<WallpaperImage>> = imageRepository.observeAllImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addQuote(text: String, author: String?) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val quote = Quote(
                id = UUID.randomUUID().toString(),
                text = text.trim(),
                author = author?.trim()?.ifBlank { null }
            )
            quoteRepository.addQuote(quote)
        }
    }

    fun deleteQuote(id: String) {
        viewModelScope.launch {
            quoteRepository.deleteQuote(id)
        }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val newImages = uris.map { uri ->
                WallpaperImage(
                    id = UUID.randomUUID().toString(),
                    uri = uri.toString()
                )
            }
            imageRepository.addImages(newImages)
        }
    }

    fun deleteImage(id: String) {
        viewModelScope.launch {
            imageRepository.deleteImage(id)
        }
    }

    class Factory(
        private val quoteRepository: QuoteRepository,
        private val imageRepository: ImageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PoolsViewModel(quoteRepository, imageRepository) as T
        }
    }
}
