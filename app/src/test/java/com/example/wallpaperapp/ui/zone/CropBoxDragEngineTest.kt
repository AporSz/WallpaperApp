package com.example.wallpaperapp.ui.zone

import com.example.wallpaperapp.domain.model.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropBoxDragEngineTest {

    private val initialRect = NormalizedRect(0.1f, 0.1f, 0.5f, 0.5f) // width = 0.4, height = 0.4

    @Test
    fun `computeDrag snaps to vertical center when raw center is near 0_5`() {
        // Center X starts at 0.3. Move accumulated Dx = +0.19f -> raw center X = 0.49f (within 0.015 of 0.5)
        val result = CropBoxDragEngine.computeDrag(
            initialRect = initialRect,
            handle = HandleType.CENTER,
            accumulatedDxNorm = 0.19f,
            accumulatedDyNorm = 0.0f
        )

        assertTrue(result.isSnappedVerticalCenter)
        // Projected displayedLeft should be 0.5 - width/2 = 0.5 - 0.2 = 0.3f
        assertEquals(0.3f, result.displayedRect.left, 0.001f)
        assertEquals(0.7f, result.displayedRect.right, 0.001f)
    }

    @Test
    fun `computeDrag breaks away cleanly past snap threshold during incremental drag`() {
        // Step 1: Small drag step into snap range (Dx = +0.19f)
        val step1 = CropBoxDragEngine.computeDrag(
            initialRect = initialRect,
            handle = HandleType.CENTER,
            accumulatedDxNorm = 0.19f,
            accumulatedDyNorm = 0.0f
        )
        assertTrue(step1.isSnappedVerticalCenter)

        // Step 2: Incremental small drag steps continuing past snap threshold (Dx = +0.192f, +0.195f, +0.22f)
        val step2 = CropBoxDragEngine.computeDrag(
            initialRect = initialRect,
            handle = HandleType.CENTER,
            accumulatedDxNorm = 0.22f, // raw center X = 0.52f (> 0.5 + 0.015)
            accumulatedDyNorm = 0.0f
        )
        assertFalse(step2.isSnappedVerticalCenter)
        // Unsnapped breakaway position: left = 0.1 + 0.22 = 0.32f
        assertEquals(0.32f, resultLeft(step2), 0.001f)
    }

    @Test
    fun `computeRawRect clamps to minWidth and minHeight on resize handles`() {
        // Resize TOP_LEFT inwards past minWidth (0.15)
        val result = CropBoxDragEngine.computeRawRect(
            initialRect = initialRect,
            handle = HandleType.TOP_LEFT,
            accumulatedDxNorm = 0.38f, // trying to shrink width down to 0.02
            accumulatedDyNorm = 0.0f,
            minWidth = 0.15f,
            minHeight = 0.10f
        )

        // Max left allowed is right (0.5) - minWidth (0.15) = 0.35
        assertEquals(0.35f, result.left, 0.001f)
        assertEquals(0.15f, result.width, 0.001f)
    }

    @Test
    fun `computeRawRect moves corner handles accurately`() {
        val result = CropBoxDragEngine.computeRawRect(
            initialRect = initialRect,
            handle = HandleType.BOTTOM_RIGHT,
            accumulatedDxNorm = 0.1f,
            accumulatedDyNorm = 0.1f
        )

        assertEquals(0.1f, result.left, 0.001f)
        assertEquals(0.1f, result.top, 0.001f)
        assertEquals(0.6f, result.right, 0.001f)
        assertEquals(0.6f, result.bottom, 0.001f)
    }

    private fun resultLeft(result: CropBoxDragEngine.DragResult): Float = result.displayedRect.left
}
