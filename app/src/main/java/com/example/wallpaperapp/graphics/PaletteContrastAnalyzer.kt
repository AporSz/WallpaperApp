package com.example.wallpaperapp.graphics

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.example.wallpaperapp.domain.engine.ContrastAnalyzer
import com.example.wallpaperapp.domain.engine.ContrastResult
import com.example.wallpaperapp.domain.model.NormalizedRect

class PaletteContrastAnalyzer : ContrastAnalyzer<Bitmap> {

    override suspend fun analyzeContrast(image: Bitmap, rect: NormalizedRect): ContrastResult {
        if (image.width <= 0 || image.height <= 0) {
            return ContrastResult(
                textColorArgb = Color.WHITE,
                shadowColorArgb = Color.BLACK,
                isDarkBackground = true
            )
        }

        val cropLeft = (rect.left * image.width).toInt().coerceIn(0, image.width - 1)
        val cropTop = (rect.top * image.height).toInt().coerceIn(0, image.height - 1)
        val cropRight = (rect.right * image.width).toInt().coerceIn(cropLeft + 1, image.width)
        val cropBottom = (rect.bottom * image.height).toInt().coerceIn(cropTop + 1, image.height)

        val width = cropRight - cropLeft
        val height = cropBottom - cropTop

        val cropped = Bitmap.createBitmap(image, cropLeft, cropTop, width, height)
        val palette = Palette.from(cropped).generate()

        // Calculate average luminance of the cropped region
        val luminance = calculateAverageLuminance(cropped, palette)

        val isDark = luminance < 0.5f

        return if (isDark) {
            ContrastResult(
                textColorArgb = Color.WHITE,
                shadowColorArgb = Color.argb(200, 0, 0, 0),
                isDarkBackground = true
            )
        } else {
            ContrastResult(
                textColorArgb = Color.argb(255, 18, 18, 18),
                shadowColorArgb = Color.argb(180, 255, 255, 255),
                isDarkBackground = false
            )
        }
    }

    private fun calculateAverageLuminance(bitmap: Bitmap, palette: Palette): Float {
        val dominantSwatch = palette.dominantSwatch
        if (dominantSwatch != null) {
            val color = dominantSwatch.rgb
            return ColorUtils.calculateLuminance(color).toFloat()
        }

        // Fallback: Sample pixels directly across bitmap
        var totalLuminance = 0.0
        val sampleStepX = (bitmap.width / 10).coerceAtLeast(1)
        val sampleStepY = (bitmap.height / 10).coerceAtLeast(1)
        var count = 0

        for (x in 0 until bitmap.width step sampleStepX) {
            for (y in 0 until bitmap.height step sampleStepY) {
                val pixel = bitmap.getPixel(x, y)
                totalLuminance += ColorUtils.calculateLuminance(pixel)
                count++
            }
        }

        return if (count > 0) (totalLuminance / count).toFloat() else 0.5f
    }
}
