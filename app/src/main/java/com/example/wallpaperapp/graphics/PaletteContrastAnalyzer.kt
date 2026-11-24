package com.example.wallpaperapp.graphics

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.example.wallpaperapp.domain.engine.ContrastAnalyzer
import com.example.wallpaperapp.domain.engine.ContrastResult
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.TextColorScheme

class PaletteContrastAnalyzer : ContrastAnalyzer<Bitmap> {

    override suspend fun analyzeContrast(
        image: Bitmap,
        rect: NormalizedRect,
        scheme: TextColorScheme
    ): ContrastResult {
        if (image.width <= 0 || image.height <= 0) {
            return ContrastResult(
                textColorArgb = Color.WHITE,
                shadowColorArgb = Color.BLACK,
                isDarkBackground = true
            )
        }

        val cropLeft = (rect.left * image.width).toInt().coerceIn(0, (image.width - 1).coerceAtLeast(0))
        val cropTop = (rect.top * image.height).toInt().coerceIn(0, (image.height - 1).coerceAtLeast(0))
        val cropRight = (rect.right * image.width).toInt().coerceIn(cropLeft + 1, image.width)
        val cropBottom = (rect.bottom * image.height).toInt().coerceIn(cropTop + 1, image.height)

        val width = (cropRight - cropLeft).coerceIn(1, image.width - cropLeft)
        val height = (cropBottom - cropTop).coerceIn(1, image.height - cropTop)

        val cropped = try {
            Bitmap.createBitmap(image, cropLeft, cropTop, width, height)
        } catch (e: Exception) {
            image
        }

        val palette = Palette.from(cropped).generate()
        val luminance = calculateAverageLuminance(cropped, palette)

        if (cropped != image) {
            try {
                cropped.recycle()
            } catch (e: Exception) {
                // Ignore
            }
        }

        val isDark = luminance < 0.5f

        return when (scheme) {
            TextColorScheme.HIGH_CONTRAST -> {
                if (isDark) {
                    ContrastResult(Color.WHITE, Color.argb(200, 0, 0, 0), true)
                } else {
                    ContrastResult(Color.argb(255, 18, 18, 18), Color.argb(180, 255, 255, 255), false)
                }
            }

            TextColorScheme.SOFT_CONTRAST -> {
                if (isDark) {
                    ContrastResult(Color.parseColor("#EAE6DF"), Color.argb(200, 0, 0, 0), true)
                } else {
                    ContrastResult(Color.parseColor("#3A3F47"), Color.argb(180, 255, 255, 255), false)
                }
            }

            TextColorScheme.WARM_AMBER -> {
                if (isDark) {
                    ContrastResult(Color.parseColor("#FDE68A"), Color.argb(200, 45, 18, 0), true)
                } else {
                    ContrastResult(Color.parseColor("#78350F"), Color.argb(180, 255, 250, 235), false)
                }
            }

            TextColorScheme.COOL_FROST -> {
                if (isDark) {
                    ContrastResult(Color.parseColor("#E0F2FE"), Color.argb(200, 10, 25, 45), true)
                } else {
                    ContrastResult(Color.parseColor("#1E3A8A"), Color.argb(180, 240, 249, 255), false)
                }
            }

            TextColorScheme.SUBTLE_PALETTE_HARMONY -> {
                val accentSwatch = palette.mutedSwatch
                    ?: palette.dominantSwatch
                    ?: palette.vibrantSwatch

                val baseColor = accentSwatch?.rgb ?: if (isDark) Color.WHITE else Color.BLACK
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(baseColor, hsl)

                if (isDark) {
                    hsl[2] = 0.88f
                    val harmonizedText = ColorUtils.HSLToColor(hsl)
                    ContrastResult(harmonizedText, Color.argb(220, 0, 0, 0), true)
                } else {
                    hsl[2] = 0.18f
                    val harmonizedText = ColorUtils.HSLToColor(hsl)
                    ContrastResult(harmonizedText, Color.argb(200, 255, 255, 255), false)
                }
            }

            TextColorScheme.VIBRANT_PALETTE_HARMONY -> {
                val accentSwatch = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.dominantSwatch

                val baseColor = accentSwatch?.rgb ?: if (isDark) Color.WHITE else Color.BLACK
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(baseColor, hsl)

                if (isDark) {
                    hsl[2] = 0.85f
                    val harmonizedText = ColorUtils.HSLToColor(hsl)
                    ContrastResult(harmonizedText, Color.argb(220, 0, 0, 0), true)
                } else {
                    hsl[2] = 0.20f
                    val harmonizedText = ColorUtils.HSLToColor(hsl)
                    ContrastResult(harmonizedText, Color.argb(200, 255, 255, 255), false)
                }
            }

            TextColorScheme.BLEND_IN -> {
                val dominantSwatch = palette.dominantSwatch
                val baseColor = dominantSwatch?.rgb ?: if (isDark) Color.BLACK else Color.WHITE
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(baseColor, hsl)

                if (isDark) {
                    hsl[2] = (hsl[2] + 0.15f).coerceAtMost(0.95f)
                    val blendText = ColorUtils.HSLToColor(hsl)
                    ContrastResult(blendText, Color.argb(120, 0, 0, 0), true)
                } else {
                    hsl[2] = (hsl[2] - 0.15f).coerceAtLeast(0.05f)
                    val blendText = ColorUtils.HSLToColor(hsl)
                    ContrastResult(blendText, Color.argb(120, 255, 255, 255), false)
                }
            }
        }
    }

    private fun calculateAverageLuminance(bitmap: Bitmap, palette: Palette): Float {
        val dominantSwatch = palette.dominantSwatch
        if (dominantSwatch != null) {
            val color = dominantSwatch.rgb
            return ColorUtils.calculateLuminance(color).toFloat()
        }

        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return 0.5f
        }

        var totalLuminance = 0.0
        val sampleStepX = (bitmap.width / 10).coerceAtLeast(1)
        val sampleStepY = (bitmap.height / 10).coerceAtLeast(1)
        var count = 0

        for (x in 0 until bitmap.width step sampleStepX) {
            for (y in 0 until bitmap.height step sampleStepY) {
                try {
                    val pixel = bitmap.getPixel(x, y)
                    totalLuminance += ColorUtils.calculateLuminance(pixel)
                    count++
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        return if (count > 0) (totalLuminance / count).toFloat() else 0.5f
    }
}
