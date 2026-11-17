package com.example.wallpaperapp.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.wallpaperapp.domain.engine.ContrastAnalyzer
import com.example.wallpaperapp.domain.engine.WallpaperRenderer
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.RenderConfig
import com.example.wallpaperapp.domain.model.TextOrientation
import java.io.File

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

        // 1. Load and downsample background image
        val backgroundBitmap = loadScaledBitmap(imageInput, targetWidth, targetHeight)
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
        val boxWidthPx = (rightPx - leftPx).toInt().coerceAtLeast(100)
        val boxHeightPx = (bottomPx - topPx).toInt().coerceAtLeast(100)

        // 5. Configure text paint with active resolved Typeface, bold, and italic styling
        val resolvedTypeface = resolveTypeface(config.fontId, config.isBold, config.isItalic)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = contrastResult.textColorArgb
            setShadowLayer(8f, 3f, 3f, contrastResult.shadowColorArgb)
            typeface = resolvedTypeface
        }

        // 6. Swap layout constraints for vertical text orientations
        val isVertical = config.textOrientation != TextOrientation.HORIZONTAL
        val layoutLineWidthPx = if (isVertical) boxHeightPx else boxWidthPx
        val layoutMaxHeightPx = if (isVertical) boxWidthPx else boxHeightPx

        val minFontSp = config.minFontSizeSp.coerceAtMost(10.0f)

        val optimalLayout = findOptimalTextLayout(
            quote = quote,
            textPaint = textPaint,
            layoutLineWidthPx = layoutLineWidthPx,
            layoutMaxHeightPx = layoutMaxHeightPx,
            minSp = minFontSp,
            maxSp = config.maxFontSizeSp,
            scaledDensity = displayMetrics.scaledDensity,
            textColor = contrastResult.textColorArgb
        )

        // 7. Draw text onto Canvas with HARD CLIPPING boundary & proper matrix translation/rotation
        val centerX = leftPx + boxWidthPx / 2f
        val centerY = topPx + boxHeightPx / 2f

        canvas.save()
        canvas.clipRect(leftPx, topPx, rightPx, bottomPx)

        when (config.textOrientation) {
            TextOrientation.HORIZONTAL -> {
                val offsetY = topPx + (boxHeightPx - optimalLayout.height) / 2f
                canvas.translate(leftPx, offsetY)
            }
            TextOrientation.VERTICAL_UP -> {
                canvas.translate(centerX, centerY)
                canvas.rotate(-90f)
                canvas.translate(-optimalLayout.width / 2f, -optimalLayout.height / 2f)
            }
            TextOrientation.VERTICAL_DOWN -> {
                canvas.translate(centerX, centerY)
                canvas.rotate(90f)
                canvas.translate(-optimalLayout.width / 2f, -optimalLayout.height / 2f)
            }
        }

        optimalLayout.draw(canvas)
        canvas.restore()

        return compositeBitmap
    }

    private fun resolveTypeface(fontId: String, isBold: Boolean, isItalic: Boolean): Typeface {
        return try {
            val baseTypeface = when (fontId) {
                "builtin_sans" -> Typeface.SANS_SERIF
                "builtin_serif" -> Typeface.SERIF
                "builtin_mono" -> Typeface.MONOSPACE
                "builtin_cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                "builtin_casual" -> Typeface.create("casual", Typeface.NORMAL)
                "builtin_sans_medium" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                "builtin_serif_bold" -> Typeface.create("serif", Typeface.BOLD)
                "builtin_mono_medium" -> Typeface.create("monospace", Typeface.BOLD)
                "builtin_sans_black" -> Typeface.create("sans-serif-black", Typeface.NORMAL)
                "builtin_sans_condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                "builtin_sans_light" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
                "builtin_serif_italic" -> Typeface.create("serif", Typeface.ITALIC)
                else -> {
                    val filePath = if (fontId.startsWith("custom_")) {
                        fontId.removePrefix("custom_")
                    } else {
                        fontId
                    }
                    val file = File(filePath)
                    if (file.exists() && file.length() > 0) {
                        Typeface.createFromFile(file)
                    } else {
                        Typeface.SANS_SERIF
                    }
                }
            }

            val styleMask = when {
                isBold && isItalic -> Typeface.BOLD_ITALIC
                isBold -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            if (styleMask != Typeface.NORMAL) {
                Typeface.create(baseTypeface, styleMask)
            } else {
                baseTypeface
            }
        } catch (e: Exception) {
            Log.e("CanvasWallpaperRenderer", "Error resolving font: $fontId, fallback to SANS_SERIF", e)
            Typeface.SANS_SERIF
        }
    }

    private fun loadScaledBitmap(input: Any, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val srcBitmap: Bitmap? = when (input) {
                is Bitmap -> input
                is Uri -> decodeUriDownsampled(input, targetWidth, targetHeight)
                is String -> decodeUriDownsampled(Uri.parse(input), targetWidth, targetHeight)
                else -> null
            }
            srcBitmap?.let { cropCenterFill(it, targetWidth, targetHeight) }
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

    private fun cropCenterFill(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        var inputBitmap = src
        var srcWidth = inputBitmap.width
        var srcHeight = inputBitmap.height

        val isTargetPortrait = targetHeight > targetWidth
        val isSourceLandscape = srcWidth > srcHeight

        // Auto-rotate landscape images 90° clockwise to fill portrait screen cleanly at high resolution
        if (isTargetPortrait && isSourceLandscape) {
            val matrix = Matrix().apply { postRotate(90f) }
            val rotated = Bitmap.createBitmap(inputBitmap, 0, 0, srcWidth, srcHeight, matrix, true)
            if (rotated != inputBitmap) {
                inputBitmap = rotated
            }
            srcWidth = inputBitmap.width
            srcHeight = inputBitmap.height
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

    private fun findOptimalTextLayout(
        quote: Quote,
        textPaint: TextPaint,
        layoutLineWidthPx: Int,
        layoutMaxHeightPx: Int,
        minSp: Float,
        maxSp: Float,
        scaledDensity: Float,
        textColor: Int
    ): StaticLayout {
        var low = minSp
        var high = maxSp
        var bestSp = minSp
        var bestLayout: StaticLayout? = null

        val formattedText = buildFormattedSpannable(quote)

        // Step/Binary search for font size
        while (high - low >= 0.5f) {
            val midSp = (low + high) / 2f
            val layout = createStaticLayout(formattedText, textPaint, midSp, quote, layoutLineWidthPx, scaledDensity, textColor)

            if (layout.height <= layoutMaxHeightPx) {
                bestSp = midSp
                bestLayout = layout
                low = midSp + 0.5f
            } else {
                high = midSp - 0.5f
            }
        }

        if (bestLayout != null) {
            return bestLayout
        }

        // At minSp, text is too tall to fit completely. Calculate maximum fitting lines and truncate with ellipsis.
        val sampleLayout = createStaticLayout(formattedText, textPaint, minSp, quote, layoutLineWidthPx, scaledDensity, textColor)
        var fitLines = 1
        for (line in 0 until sampleLayout.lineCount) {
            if (sampleLayout.getLineBottom(line) <= layoutMaxHeightPx) {
                fitLines = line + 1
            } else {
                break
            }
        }

        return createStaticLayout(
            text = formattedText,
            basePaint = textPaint,
            fontSizeSp = minSp,
            quote = quote,
            boxWidthPx = layoutLineWidthPx,
            scaledDensity = scaledDensity,
            textColor = textColor,
            maxLines = fitLines,
            ellipsize = TextUtils.TruncateAt.END
        )
    }

    private fun buildFormattedSpannable(quote: Quote): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        val mainText = "“${quote.text}”"
        builder.append(mainText)

        if (!quote.author.isNullOrBlank()) {
            builder.append("\n\n— ${quote.author.trim()}")
        }
        return builder
    }

    private fun createStaticLayout(
        text: SpannableStringBuilder,
        basePaint: TextPaint,
        fontSizeSp: Float,
        quote: Quote,
        boxWidthPx: Int,
        scaledDensity: Float,
        textColor: Int,
        maxLines: Int = Int.MAX_VALUE,
        ellipsize: TextUtils.TruncateAt? = null
    ): StaticLayout {
        val quoteFontSizePx = (fontSizeSp * scaledDensity).toInt().coerceAtLeast(1)
        val authorFontSizePx = ((fontSizeSp * 0.75f) * scaledDensity).toInt().coerceAtLeast(1)

        val mainText = "“${quote.text}”"
        val spannable = SpannableStringBuilder(text)

        // Set quote font size and style
        spannable.setSpan(
            AbsoluteSizeSpan(quoteFontSizePx),
            0,
            mainText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            mainText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(textColor),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set author font size and style if present
        if (!quote.author.isNullOrBlank()) {
            val authorStart = mainText.length + 2 // after \n\n
            if (authorStart < spannable.length) {
                spannable.setSpan(
                    AbsoluteSizeSpan(authorFontSizePx),
                    authorStart,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    authorStart,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        val builder = StaticLayout.Builder.obtain(spannable, 0, spannable.length, basePaint, boxWidthPx)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(true)

        if (maxLines != Int.MAX_VALUE) {
            builder.setMaxLines(maxLines)
        }
        if (ellipsize != null) {
            builder.setEllipsize(ellipsize)
        }

        return builder.build()
    }
}
