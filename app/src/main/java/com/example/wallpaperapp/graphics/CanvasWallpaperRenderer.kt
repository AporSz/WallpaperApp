package com.example.wallpaperapp.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.example.wallpaperapp.domain.engine.ContrastAnalyzer
import com.example.wallpaperapp.domain.engine.WallpaperRenderer
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

        // 1. Load and downsample background image
        val backgroundBitmap = loadScaledBitmap(imageInput, targetWidth, targetHeight)
            ?: createDefaultBackground(targetWidth, targetHeight)

        // 2. Analyze contrast of background in the cropped bounding box
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

        // 5. Binary search for optimal font size that fits inside boxHeightPx
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = contrastResult.textColorArgb
            setShadowLayer(8f, 3f, 3f, contrastResult.shadowColorArgb)
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        val optimalLayout = findOptimalTextLayout(
            quote = quote,
            textPaint = textPaint,
            boxWidthPx = boxWidthPx,
            boxHeightPx = boxHeightPx,
            minSp = config.minFontSizeSp,
            maxSp = config.maxFontSizeSp,
            scaledDensity = displayMetrics.scaledDensity,
            textColor = contrastResult.textColorArgb
        )

        // 6. Draw text centered vertically inside bounding box
        val offsetY = topPx + (boxHeightPx - optimalLayout.height) / 2f
        canvas.save()
        canvas.translate(leftPx, offsetY)
        optimalLayout.draw(canvas)
        canvas.restore()

        return compositeBitmap
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
            null
        }
    }

    private fun decodeUriDownsampled(uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(options.outWidth, options.outHeight, targetWidth, targetHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
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
        val srcWidth = src.width
        val srcHeight = src.height

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
        canvas.drawBitmap(src, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }

    private fun findOptimalTextLayout(
        quote: Quote,
        textPaint: TextPaint,
        boxWidthPx: Int,
        boxHeightPx: Int,
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
            val layout = createStaticLayout(formattedText, textPaint, midSp, quote, boxWidthPx, scaledDensity, textColor)

            if (layout.height <= boxHeightPx) {
                bestSp = midSp
                bestLayout = layout
                low = midSp + 0.5f
            } else {
                high = midSp - 0.5f
            }
        }

        return bestLayout ?: createStaticLayout(formattedText, textPaint, minSp, quote, boxWidthPx, scaledDensity, textColor)
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
        textColor: Int
    ): StaticLayout {
        val quoteFontSizePx = (fontSizeSp * scaledDensity).toInt()
        val authorFontSizePx = ((fontSizeSp * 0.75f) * scaledDensity).toInt()

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

        return StaticLayout.Builder.obtain(spannable, 0, spannable.length, basePaint, boxWidthPx)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(true)
            .build()
    }

    private fun createDefaultBackground(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.DKGRAY)
        return bitmap
    }
}
