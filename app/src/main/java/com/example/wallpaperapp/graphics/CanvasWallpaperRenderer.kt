package com.example.wallpaperapp.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.wallpaperapp.domain.engine.ContrastAnalyzer
import com.example.wallpaperapp.domain.engine.WallpaperRenderer
import com.example.wallpaperapp.domain.model.LandscapeHandlingMode
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.RenderConfig

class CanvasWallpaperRenderer(
    private val context: Context,
    private val contrastAnalyzer: ContrastAnalyzer<Bitmap> = PaletteContrastAnalyzer()
) : WallpaperRenderer<Any, Bitmap> {

    override suspend fun renderWallpaper(
        imageInput: Any,
        quote: Quote,
        config: RenderConfig
    ): Bitmap? {
        val displayMetrics = context.resources.displayMetrics
        val targetWidth = displayMetrics.widthPixels.coerceAtLeast(720)
        val targetHeight = displayMetrics.heightPixels.coerceAtLeast(1280)

        // 1. Load and downsample background image respecting LandscapeHandlingMode
        val backgroundBitmap = loadScaledBitmap(imageInput, targetWidth, targetHeight, config.landscapeHandlingMode)
            ?: run {
                Log.e("CanvasWallpaperRenderer", "Failed to load background bitmap for input: $imageInput")
                return null
            }

        // 2. Analyze contrast of background in the unrotated cropped bounding box
        val contrastResult = contrastAnalyzer.analyzeContrast(backgroundBitmap, config.targetRect)

        // 3. Prepare mutable composite bitmap & canvas
        val compositeBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)

        // 4. Calculate bounding box in pixels
        val leftPx = config.targetRect.left * targetWidth
        val topPx = config.targetRect.top * targetHeight
        val rightPx = config.targetRect.right * targetWidth
        val bottomPx = config.targetRect.bottom * targetHeight

        // 5. Render quote text onto Canvas using single source of truth drawer engine
        QuoteTextCanvasDrawer.drawQuoteOnCanvas(
            canvas = canvas,
            quote = quote,
            leftPx = leftPx,
            topPx = topPx,
            rightPx = rightPx,
            bottomPx = bottomPx,
            orientation = config.textOrientation,
            fontId = config.fontId,
            isBold = config.isBold,
            isItalic = config.isItalic,
            textColorArgb = contrastResult.textColorArgb,
            shadowColorArgb = contrastResult.shadowColorArgb,
            scaledDensity = displayMetrics.scaledDensity
        )

        return compositeBitmap
    }

    private fun loadScaledBitmap(input: Any, targetWidth: Int, targetHeight: Int, mode: LandscapeHandlingMode): Bitmap? {
        return try {
            val srcBitmap: Bitmap? = when (input) {
                is Bitmap -> input
                is Uri -> decodeUriDownsampled(input, targetWidth, targetHeight)
                is String -> decodeUriDownsampled(Uri.parse(input), targetWidth, targetHeight)
                else -> null
            }
            srcBitmap?.let { cropCenterFill(it, targetWidth, targetHeight, mode) }
        } catch (e: Exception) {
            Log.e("CanvasWallpaperRenderer", "Error loading scaled bitmap", e)
            null
        }
    }

    private fun decodeUriDownsampled(uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e("CanvasWallpaperRenderer", "Failed to decode bounds for URI: $uri")
                return null
            }

            val sampleSize = calculateInSampleSize(options.outWidth, options.outHeight, targetWidth, targetHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val rotationDegrees = getExifRotation(uri)
            if (rotationDegrees != 0f) {
                rotateBitmap(decodedBitmap, rotationDegrees)
            } else {
                decodedBitmap
            }
        } catch (e: Exception) {
            Log.e("CanvasWallpaperRenderer", "Error decoding URI: $uri", e)
            null
        }
    }

    private fun getExifRotation(uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (rotated != src) {
            src.recycle()
        }
        return rotated
    }

    private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var sampleSize = 1
        if (srcH > reqH || srcW > reqW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / sampleSize) >= reqH && (halfW / sampleSize) >= reqW) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    private fun cropCenterFill(src: Bitmap, targetWidth: Int, targetHeight: Int, mode: LandscapeHandlingMode): Bitmap {
        var inputBitmap = src
        var srcWidth = inputBitmap.width
        var srcHeight = inputBitmap.height

        val isTargetPortrait = targetHeight > targetWidth
        val isSourceLandscape = srcWidth > srcHeight

        if (isTargetPortrait && isSourceLandscape) {
            val rotationDegrees = when (mode) {
                LandscapeHandlingMode.ROTATE_90_CW -> 90f
                LandscapeHandlingMode.ROTATE_90_CCW -> -90f
                LandscapeHandlingMode.CROP -> 0f
            }

            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(inputBitmap, 0, 0, srcWidth, srcHeight, matrix, true)
                if (rotated != inputBitmap) {
                    inputBitmap = rotated
                }
                srcWidth = inputBitmap.width
                srcHeight = inputBitmap.height
            }
        }

        val scale = maxOf(
            targetWidth.toFloat() / srcWidth,
            targetHeight.toFloat() / srcHeight
        )

        val scaledW = scale * srcWidth
        val scaledH = scale * srcHeight

        val dx = (targetWidth - scaledW) / 2f
        val dy = (targetHeight - scaledH) / 2f

        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(inputBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }
}
