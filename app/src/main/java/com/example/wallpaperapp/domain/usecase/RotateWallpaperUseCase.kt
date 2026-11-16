package com.example.wallpaperapp.domain.usecase

import android.graphics.Bitmap
import com.example.wallpaperapp.domain.applier.WallpaperApplier
import com.example.wallpaperapp.domain.engine.ShuffleEngine
import com.example.wallpaperapp.domain.engine.WallpaperRenderer
import com.example.wallpaperapp.domain.model.Quote
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
    private val wallpaperApplier: WallpaperApplier<Bitmap>
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
            val quoteQueue = settingsRepository.observeQuoteShuffleQueue().first()
            val imageQueue = settingsRepository.observeImageShuffleQueue().first()

            val quoteResult = quoteShuffleEngine.selectNext(quotes, quoteQueue) { it.id }
                ?: return Result.failure(IllegalStateException("Failed to select next quote"))

            val imageResult = imageShuffleEngine.selectNext(images, imageQueue) { it.id }
                ?: return Result.failure(IllegalStateException("Failed to select next image"))

            val (selectedQuote, updatedQuoteQueue) = quoteResult
            val (selectedImage, updatedImageQueue) = imageResult

            val renderConfig = RenderConfig(targetRect = rect, textOrientation = orientation)
            val renderedBitmap = wallpaperRenderer.renderWallpaper(selectedImage.uri, selectedQuote, renderConfig)
                ?: return Result.failure(IllegalStateException("Failed to render composite wallpaper"))

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
