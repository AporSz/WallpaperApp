package com.example.wallpaperapp.domain.usecase

import android.graphics.Bitmap
import com.example.wallpaperapp.data.engine.ShuffleBagEngine
import com.example.wallpaperapp.domain.applier.WallpaperApplier
import com.example.wallpaperapp.domain.engine.ShuffleEngine
import com.example.wallpaperapp.domain.engine.WallpaperRenderer
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.QuoteFont
import com.example.wallpaperapp.domain.model.RenderConfig
import com.example.wallpaperapp.domain.model.WallpaperImage
import com.example.wallpaperapp.domain.repository.ImageRepository
import com.example.wallpaperapp.domain.repository.QuoteRepository
import com.example.wallpaperapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class RotateWallpaperUseCase(
    private val quoteRepository: QuoteRepository,
    private val imageRepository: ImageRepository,
    private val settingsRepository: SettingsRepository,
    private val quoteShuffleEngine: ShuffleEngine<Quote>,
    private val imageShuffleEngine: ShuffleEngine<WallpaperImage>,
    private val wallpaperRenderer: WallpaperRenderer<Any, Bitmap>,
    private val wallpaperApplier: WallpaperApplier<Bitmap>,
    private val fontShuffleEngine: ShuffleEngine<QuoteFont> = ShuffleBagEngine()
) {

    suspend operator fun invoke(): Result<Boolean> {
        return try {
            val quotes = quoteRepository.getAllQuotes()
            if (quotes.isEmpty()) {
                return Result.failure(IllegalStateException("No quotes available in pool"))
            }

            val images = imageRepository.getAllImages()
            if (images.isEmpty()) {
                return Result.failure(IllegalStateException("No images available in pool"))
            }

            val targetScreen = settingsRepository.observeWallpaperTarget().first()
            val rect = settingsRepository.observeNormalizedRect().first()
            val orientation = settingsRepository.observeTextOrientation().first()
            val isBold = settingsRepository.observeIsBold().first()
            val isItalic = settingsRepository.observeIsItalic().first()
            val shuffleFonts = settingsRepository.observeShuffleFonts().first()
            val landscapeHandlingMode = settingsRepository.observeLandscapeHandlingMode().first()
            val textColorScheme = settingsRepository.observeTextColorScheme().first()

            val activeFontId = if (shuffleFonts) {
                val fontQueue = settingsRepository.observeFontShuffleQueue().first()
                val fontResult = fontShuffleEngine.selectNext(QuoteFont.BUILTIN_FONTS, fontQueue) { it.id }
                if (fontResult != null) {
                    val (selectedFont, updatedFontQueue) = fontResult
                    settingsRepository.setFontShuffleQueue(updatedFontQueue)
                    selectedFont.id
                } else {
                    settingsRepository.observeQuoteFontId().first()
                }
            } else {
                settingsRepository.observeQuoteFontId().first()
            }

            val quoteQueue = settingsRepository.observeQuoteShuffleQueue().first()
            val imageQueue = settingsRepository.observeImageShuffleQueue().first()

            val quoteResult = quoteShuffleEngine.selectNext(quotes, quoteQueue) { it.id }
                ?: return Result.failure(IllegalStateException("Failed to select next quote"))

            val (selectedQuote, updatedQuoteQueue) = quoteResult

            var attempts = 0
            var renderedBitmap: Bitmap? = null
            var updatedImageQueue = imageQueue

            val candidateImages = images.toMutableList()

            while (renderedBitmap == null && candidateImages.isNotEmpty() && attempts < images.size) {
                attempts++
                val imageResult = imageShuffleEngine.selectNext(candidateImages, updatedImageQueue) { it.id }
                    ?: break

                val (candidateImage, nextImageQueue) = imageResult
                updatedImageQueue = nextImageQueue

                val renderConfig = RenderConfig(
                    targetRect = rect,
                    textOrientation = orientation,
                    fontId = activeFontId,
                    isBold = isBold,
                    isItalic = isItalic,
                    landscapeHandlingMode = landscapeHandlingMode,
                    textColorScheme = textColorScheme
                )
                renderedBitmap = wallpaperRenderer.renderWallpaper(candidateImage.uri, selectedQuote, renderConfig)

                if (renderedBitmap == null) {
                    // Unreadable or revoked URI! Prune from candidate images list and delete invalid entry from repository
                    candidateImages.removeAll { it.id == candidateImage.id }
                    try {
                        imageRepository.deleteImage(candidateImage.id)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }

            if (renderedBitmap == null) {
                return Result.failure(IllegalStateException("Failed to load or render any valid background image"))
            }

            val applied = wallpaperApplier.applyWallpaper(renderedBitmap, targetScreen)
            if (applied) {
                settingsRepository.setQuoteShuffleQueue(updatedQuoteQueue)
                settingsRepository.setImageShuffleQueue(updatedImageQueue)
                Result.success(true)
            } else {
                Result.failure(RuntimeException("System failed to apply wallpaper"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
