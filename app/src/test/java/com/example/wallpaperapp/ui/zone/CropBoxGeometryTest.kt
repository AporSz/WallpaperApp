package com.example.wallpaperapp.ui.zone

import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.ui.zone.CropBoxGeometry.resizeOrMove
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropBoxGeometryTest {

    @Test
    fun `center drag moves box within boundaries`() {
        val initial = NormalizedRect(0.2f, 0.2f, 0.6f, 0.6f) // 0.4 x 0.4
        val moved = initial.resizeOrMove(HandleType.CENTER, 0.1f, 0.1f)

        assertEquals(0.3f, moved.left, 0.001f)
        assertEquals(0.3f, moved.top, 0.001f)
        assertEquals(0.7f, moved.right, 0.001f)
        assertEquals(0.7f, moved.bottom, 0.001f)
    }

    @Test
    fun `center drag clamps at screen edges`() {
        val initial = NormalizedRect(0.7f, 0.7f, 0.9f, 0.9f) // 0.2 x 0.2
        val moved = initial.resizeOrMove(HandleType.CENTER, 0.5f, 0.5f)

        assertEquals(0.8f, moved.left, 0.001f)
        assertEquals(0.8f, moved.top, 0.001f)
        assertEquals(1.0f, moved.right, 0.001f)
        assertEquals(1.0f, moved.bottom, 0.001f)
    }

    @Test
    fun `right edge drag extends width independently`() {
        val initial = NormalizedRect(0.1f, 0.1f, 0.5f, 0.5f)
        val resized = initial.resizeOrMove(HandleType.RIGHT, 0.2f, 0f)

        assertEquals(0.1f, resized.left, 0.001f)
        assertEquals(0.1f, resized.top, 0.001f)
        assertEquals(0.7f, resized.right, 0.001f)
        assertEquals(0.5f, resized.bottom, 0.001f)
    }

    @Test
    fun `top edge drag shrinks height respecting minHeight constraint`() {
        val initial = NormalizedRect(0.1f, 0.2f, 0.5f, 0.5f)
        val resized = initial.resizeOrMove(HandleType.TOP, 0f, 0.4f)
        assertEquals(0.1f, resized.left, 0.001f)
        assertEquals(0.4f, resized.top, 0.001f)
        assertEquals(0.5f, resized.right, 0.001f)
        assertEquals(0.5f, resized.bottom, 0.001f)
        assertTrue(resized.height >= 0.099f)
    }

    @Test
    fun `bottom-right corner drag resizes both dimensions`() {
        val initial = NormalizedRect(0.1f, 0.1f, 0.5f, 0.5f)
        val resized = initial.resizeOrMove(HandleType.BOTTOM_RIGHT, 0.1f, 0.1f)

        assertEquals(0.1f, resized.left, 0.001f)
        assertEquals(0.1f, resized.top, 0.001f)
        assertEquals(0.6f, resized.right, 0.001f)
        assertEquals(0.6f, resized.bottom, 0.001f)
    }
}
