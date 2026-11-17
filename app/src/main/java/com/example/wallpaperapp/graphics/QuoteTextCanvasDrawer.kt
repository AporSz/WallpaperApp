package com.example.wallpaperapp.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
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
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.TextOrientation
import java.io.File

object QuoteTextCanvasDrawer {

    fun resolveTypeface(fontId: String, isBold: Boolean, isItalic: Boolean): Typeface {
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
            Log.e("QuoteTextCanvasDrawer", "Error resolving font: $fontId, fallback to SANS_SERIF", e)
            Typeface.SANS_SERIF
        }
    }

    fun drawQuoteOnCanvas(
        canvas: Canvas,
        quote: Quote,
        leftPx: Float,
        topPx: Float,
        rightPx: Float,
        bottomPx: Float,
        orientation: TextOrientation,
        fontId: String,
        isBold: Boolean,
        isItalic: Boolean,
        textColorArgb: Int,
        shadowColorArgb: Int,
        scaledDensity: Float
    ) {
        val boxWidthPx = (rightPx - leftPx).toInt().coerceAtLeast(50)
        val boxHeightPx = (bottomPx - topPx).toInt().coerceAtLeast(50)

        val resolvedTypeface = resolveTypeface(fontId, isBold, isItalic)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorArgb
            setShadowLayer(8f, 3f, 3f, shadowColorArgb)
            typeface = resolvedTypeface
        }

        val isVertical = orientation != TextOrientation.HORIZONTAL
        val layoutLineWidthPx = if (isVertical) boxHeightPx else boxWidthPx
        val layoutMaxHeightPx = if (isVertical) boxWidthPx else boxHeightPx

        val minFontSp = 10.0f
        val maxFontSp = ((layoutMaxHeightPx / scaledDensity) * 0.75f).coerceIn(12.0f, 150.0f)

        val optimalLayout = findOptimalTextLayout(
            quote = quote,
            textPaint = textPaint,
            layoutLineWidthPx = layoutLineWidthPx,
            layoutMaxHeightPx = layoutMaxHeightPx,
            minSp = minFontSp,
            maxSp = maxFontSp,
            scaledDensity = scaledDensity,
            textColor = textColorArgb
        )

        val centerX = leftPx + boxWidthPx / 2f
        val centerY = topPx + boxHeightPx / 2f

        canvas.save()
        canvas.clipRect(leftPx, topPx, rightPx, bottomPx)

        when (orientation) {
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

        if (!quote.author.isNullOrBlank()) {
            val authorStart = mainText.length + 2
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
