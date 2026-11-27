package com.example.wallpaperapp.ui.zone

import com.example.wallpaperapp.domain.model.NormalizedRect
import kotlin.math.abs

object CropBoxDragEngine {

    const val DEFAULT_MIN_WIDTH_NORM = 0.15f
    const val DEFAULT_MIN_HEIGHT_NORM = 0.10f
    const val DEFAULT_SNAP_THRESHOLD = 0.015f

    data class DragResult(
        val rawRect: NormalizedRect,
        val displayedRect: NormalizedRect,
        val isSnappedVerticalCenter: Boolean,
        val isSnappedHorizontalCenter: Boolean
    )

    fun computeRawRect(
        initialRect: NormalizedRect,
        handle: HandleType,
        accumulatedDxNorm: Float,
        accumulatedDyNorm: Float,
        minWidth: Float = DEFAULT_MIN_WIDTH_NORM,
        minHeight: Float = DEFAULT_MIN_HEIGHT_NORM
    ): NormalizedRect {
        return when (handle) {
            HandleType.CENTER -> {
                val w = initialRect.width
                val h = initialRect.height
                val newLeft = (initialRect.left + accumulatedDxNorm).coerceIn(0f, 1f - w)
                val newTop = (initialRect.top + accumulatedDyNorm).coerceIn(0f, 1f - h)
                val newRight = (newLeft + w).coerceIn(newLeft + minWidth, 1f)
                val newBottom = (newTop + h).coerceIn(newTop + minHeight, 1f)
                NormalizedRect(newLeft, newTop, newRight, newBottom)
            }
            HandleType.TOP_LEFT -> {
                val newLeft = (initialRect.left + accumulatedDxNorm).coerceIn(0f, initialRect.right - minWidth)
                val newTop = (initialRect.top + accumulatedDyNorm).coerceIn(0f, initialRect.bottom - minHeight)
                NormalizedRect(newLeft, newTop, initialRect.right, initialRect.bottom)
            }
            HandleType.TOP -> {
                val newTop = (initialRect.top + accumulatedDyNorm).coerceIn(0f, initialRect.bottom - minHeight)
                NormalizedRect(initialRect.left, newTop, initialRect.right, initialRect.bottom)
            }
            HandleType.TOP_RIGHT -> {
                val newRight = (initialRect.right + accumulatedDxNorm).coerceIn(initialRect.left + minWidth, 1f)
                val newTop = (initialRect.top + accumulatedDyNorm).coerceIn(0f, initialRect.bottom - minHeight)
                NormalizedRect(initialRect.left, newTop, newRight, initialRect.bottom)
            }
            HandleType.RIGHT -> {
                val newRight = (initialRect.right + accumulatedDxNorm).coerceIn(initialRect.left + minWidth, 1f)
                NormalizedRect(initialRect.left, initialRect.top, newRight, initialRect.bottom)
            }
            HandleType.BOTTOM_RIGHT -> {
                val newRight = (initialRect.right + accumulatedDxNorm).coerceIn(initialRect.left + minWidth, 1f)
                val newBottom = (initialRect.bottom + accumulatedDyNorm).coerceIn(initialRect.top + minHeight, 1f)
                NormalizedRect(initialRect.left, initialRect.top, newRight, newBottom)
            }
            HandleType.BOTTOM -> {
                val newBottom = (initialRect.bottom + accumulatedDyNorm).coerceIn(initialRect.top + minHeight, 1f)
                NormalizedRect(initialRect.left, initialRect.top, initialRect.right, newBottom)
            }
            HandleType.BOTTOM_LEFT -> {
                val newLeft = (initialRect.left + accumulatedDxNorm).coerceIn(0f, initialRect.right - minWidth)
                val newBottom = (initialRect.bottom + accumulatedDyNorm).coerceIn(initialRect.top + minHeight, 1f)
                NormalizedRect(newLeft, initialRect.top, initialRect.right, newBottom)
            }
            HandleType.LEFT -> {
                val newLeft = (initialRect.left + accumulatedDxNorm).coerceIn(0f, initialRect.right - minWidth)
                NormalizedRect(newLeft, initialRect.top, initialRect.right, initialRect.bottom)
            }
        }
    }

    fun computeDrag(
        initialRect: NormalizedRect,
        handle: HandleType,
        accumulatedDxNorm: Float,
        accumulatedDyNorm: Float,
        minWidth: Float = DEFAULT_MIN_WIDTH_NORM,
        minHeight: Float = DEFAULT_MIN_HEIGHT_NORM,
        snapThreshold: Float = DEFAULT_SNAP_THRESHOLD
    ): DragResult {
        val raw = computeRawRect(
            initialRect = initialRect,
            handle = handle,
            accumulatedDxNorm = accumulatedDxNorm,
            accumulatedDyNorm = accumulatedDyNorm,
            minWidth = minWidth,
            minHeight = minHeight
        )

        var displayedLeft = raw.left
        var displayedTop = raw.top
        var isSnappedVert = false
        var isSnappedHoriz = false

        if (handle == HandleType.CENTER) {
            val rawCenterX = raw.left + raw.width / 2f
            val rawCenterY = raw.top + raw.height / 2f

            if (abs(rawCenterX - 0.5f) < snapThreshold) {
                displayedLeft = 0.5f - raw.width / 2f
                isSnappedVert = true
            }

            if (abs(rawCenterY - 0.5f) < snapThreshold) {
                displayedTop = 0.5f - raw.height / 2f
                isSnappedHoriz = true
            }
        }

        val displayedRight = displayedLeft + raw.width
        val displayedBottom = displayedTop + raw.height

        val displayed = NormalizedRect(displayedLeft, displayedTop, displayedRight, displayedBottom)

        return DragResult(
            rawRect = raw,
            displayedRect = displayed,
            isSnappedVerticalCenter = isSnappedVert,
            isSnappedHorizontalCenter = isSnappedHoriz
        )
    }
}
