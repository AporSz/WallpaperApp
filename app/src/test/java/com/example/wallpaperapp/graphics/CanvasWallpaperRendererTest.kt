package com.example.wallpaperapp.graphics

import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.RenderConfig
import com.example.wallpaperapp.domain.model.TextOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CanvasWallpaperRendererTest {

    @Test
    fun `renderConfig initialization retains text orientation and rect`() {
        val rect = NormalizedRect(0.1f, 0.2f, 0.8f, 0.8f)
        val config = RenderConfig(
            targetRect = rect,
            textOrientation = TextOrientation.VERTICAL_UP
        )

        assertNotNull(config)
        assertEquals(TextOrientation.VERTICAL_UP, config.textOrientation)
        assertEquals(rect, config.targetRect)
    }

    @Test
    fun `very long quote creates non-null formatted quote`() {
        val longQuoteText = "A ".repeat(500)
        val quote = Quote("long_1", longQuoteText, "Author Name")
        assertNotNull(quote)
        assertEquals(1000, quote.text.length)
    }
}
