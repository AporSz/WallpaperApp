package com.example.wallpaperapp.ui.zone

import androidx.compose.ui.geometry.Offset
import com.example.wallpaperapp.domain.model.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CropBoxHitTesterTest {

    private val rect = NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f)
    private val containerWidth = 1000f
    private val containerHeight = 1000f
    private val cornerRadiusPx = 40f
    private val edgeBandPx = 30f

    @Test
    fun `hitTest returns corner handle when touch is within corner radius`() {
        // TOP_LEFT is at (200, 200)
        val hitTopLeft = CropBoxHitTester.hitTest(
            rect = rect,
            containerWidthPx = containerWidth,
            containerHeightPx = containerHeight,
            touchPx = Offset(210f, 210f),
            cornerRadiusPx = cornerRadiusPx,
            edgeBandPx = edgeBandPx
        )
        assertEquals(HandleType.TOP_LEFT, hitTopLeft)

        // BOTTOM_RIGHT is at (800, 800)
        val hitBottomRight = CropBoxHitTester.hitTest(
            rect = rect,
            containerWidthPx = containerWidth,
            containerHeightPx = containerHeight,
            touchPx = Offset(790f, 790f),
            cornerRadiusPx = cornerRadiusPx,
            edgeBandPx = edgeBandPx
        )
        assertEquals(HandleType.BOTTOM_RIGHT, hitBottomRight)
    }

    @Test
    fun `hitTest returns edge handle when touch is near edge line`() {
        // TOP edge is y = 200, x between 200 and 800
        val hitTop = CropBoxHitTester.hitTest(
            rect = rect,
            containerWidthPx = containerWidth,
            containerHeightPx = containerHeight,
            touchPx = Offset(500f, 210f),
            cornerRadiusPx = cornerRadiusPx,
            edgeBandPx = edgeBandPx
        )
        assertEquals(HandleType.TOP, hitTop)

        // LEFT edge is x = 200, y between 200 and 800
        val hitLeft = CropBoxHitTester.hitTest(
            rect = rect,
            containerWidthPx = containerWidth,
            containerHeightPx = containerHeight,
            touchPx = Offset(205f, 500f),
            cornerRadiusPx = cornerRadiusPx,
            edgeBandPx = edgeBandPx
        )
        assertEquals(HandleType.LEFT, hitLeft)
    }

    @Test
    fun `hitTest returns CENTER when touch is inside box interior`() {
        // Center is at (500, 500)
        val hitCenter = CropBoxHitTester.hitTest(
            rect = rect,
            containerWidthPx = containerWidth,
            containerHeightPx = containerHeight,
            touchPx = Offset(500f, 500f),
            cornerRadiusPx = cornerRadiusPx,
            edgeBandPx = edgeBandPx
        )
        assertEquals(HandleType.CENTER, hitCenter)
    }

    @Test
    fun `hitTest returns null when touch is outside bounding box and handles`() {
        // Outside at (50, 50)
        val hitBackground = CropBoxHitTester.hitTest(
            rect = rect,
            containerWidthPx = containerWidth,
            containerHeightPx = containerHeight,
            touchPx = Offset(50f, 50f),
            cornerRadiusPx = cornerRadiusPx,
            edgeBandPx = edgeBandPx
        )
        assertNull(hitBackground)
    }
}
