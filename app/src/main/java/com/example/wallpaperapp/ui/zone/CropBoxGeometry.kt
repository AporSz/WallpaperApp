package com.example.wallpaperapp.ui.zone

import com.example.wallpaperapp.domain.model.NormalizedRect
import kotlin.math.abs

enum class HandleType {
    TOP_LEFT,
    TOP,
    TOP_RIGHT,
    RIGHT,
    BOTTOM_RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    LEFT,
    CENTER
}

object CropBoxGeometry {

    const val MIN_WIDTH_NORM = 0.15f
    const val MIN_HEIGHT_NORM = 0.10f

    fun NormalizedRect.resizeOrMove(
        handle: HandleType,
        dxNorm: Float,
        dyNorm: Float,
        minWidth: Float = MIN_WIDTH_NORM,
        minHeight: Float = MIN_HEIGHT_NORM
    ): NormalizedRect {
        return when (handle) {
            HandleType.CENTER -> {
                val currentWidth = width
                val currentHeight = height
                var newLeft = (left + dxNorm).coerceIn(0.0f, 1.0f - currentWidth)
                var newTop = (top + dyNorm).coerceIn(0.0f, 1.0f - currentHeight)

                // Magnetic center snapping logic with smooth breakaway threshold
                val centerSnapThreshold = 0.015f
                if (abs(newLeft + currentWidth / 2f - 0.5f) < centerSnapThreshold) {
                    newLeft = 0.5f - currentWidth / 2f
                }
                if (abs(newTop + currentHeight / 2f - 0.5f) < centerSnapThreshold) {
                    newTop = 0.5f - currentHeight / 2f
                }

                val newRight = (newLeft + currentWidth).coerceIn(newLeft + minWidth, 1.0f)
                val newBottom = (newTop + currentHeight).coerceIn(newTop + minHeight, 1.0f)
                NormalizedRect(newLeft, newTop, newRight, newBottom)
            }
            HandleType.TOP_LEFT -> {
                val newLeft = (left + dxNorm).coerceIn(0.0f, right - minWidth)
                val newTop = (top + dyNorm).coerceIn(0.0f, bottom - minHeight)
                NormalizedRect(newLeft, newTop, right, bottom)
            }
            HandleType.TOP -> {
                val newTop = (top + dyNorm).coerceIn(0.0f, bottom - minHeight)
                NormalizedRect(left, newTop, right, bottom)
            }
            HandleType.TOP_RIGHT -> {
                val newRight = (right + dxNorm).coerceIn(left + minWidth, 1.0f)
                val newTop = (top + dyNorm).coerceIn(0.0f, bottom - minHeight)
                NormalizedRect(left, newTop, newRight, bottom)
            }
            HandleType.RIGHT -> {
                val newRight = (right + dxNorm).coerceIn(left + minWidth, 1.0f)
                NormalizedRect(left, top, newRight, bottom)
            }
            HandleType.BOTTOM_RIGHT -> {
                val newRight = (right + dxNorm).coerceIn(left + minWidth, 1.0f)
                val newBottom = (bottom + dyNorm).coerceIn(top + minHeight, 1.0f)
                NormalizedRect(left, top, newRight, newBottom)
            }
            HandleType.BOTTOM -> {
                val newBottom = (bottom + dyNorm).coerceIn(top + minHeight, 1.0f)
                NormalizedRect(left, top, right, newBottom)
            }
            HandleType.BOTTOM_LEFT -> {
                val newLeft = (left + dxNorm).coerceIn(0.0f, right - minWidth)
                val newBottom = (bottom + dyNorm).coerceIn(top + minHeight, 1.0f)
                NormalizedRect(newLeft, top, right, newBottom)
            }
            HandleType.LEFT -> {
                val newLeft = (left + dxNorm).coerceIn(0.0f, right - minWidth)
                NormalizedRect(newLeft, top, right, bottom)
            }
        }
    }
}
