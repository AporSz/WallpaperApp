package com.example.wallpaperapp.ui.zone

import androidx.compose.ui.geometry.Offset
import com.example.wallpaperapp.domain.model.NormalizedRect
import kotlin.math.abs
import kotlin.math.hypot

object CropBoxHitTester {

    fun hitTest(
        rect: NormalizedRect,
        containerWidthPx: Float,
        containerHeightPx: Float,
        touchPx: Offset,
        cornerRadiusPx: Float,
        edgeBandPx: Float
    ): HandleType? {
        if (containerWidthPx <= 0f || containerHeightPx <= 0f) return null

        val leftPx = rect.left * containerWidthPx
        val topPx = rect.top * containerHeightPx
        val rightPx = rect.right * containerWidthPx
        val bottomPx = rect.bottom * containerHeightPx

        // Priority 1: Check 4 Corners (Corner Radius)
        if (hypot(touchPx.x - leftPx, touchPx.y - topPx) <= cornerRadiusPx) return HandleType.TOP_LEFT
        if (hypot(touchPx.x - rightPx, touchPx.y - topPx) <= cornerRadiusPx) return HandleType.TOP_RIGHT
        if (hypot(touchPx.x - leftPx, touchPx.y - bottomPx) <= cornerRadiusPx) return HandleType.BOTTOM_LEFT
        if (hypot(touchPx.x - rightPx, touchPx.y - bottomPx) <= cornerRadiusPx) return HandleType.BOTTOM_RIGHT

        // Priority 2: Check 4 Edges (Perpendicular Edge Hit Band)
        val inHorizontalBounds = touchPx.x >= leftPx - edgeBandPx && touchPx.x <= rightPx + edgeBandPx
        val inVerticalBounds = touchPx.y >= topPx - edgeBandPx && touchPx.y <= bottomPx + edgeBandPx

        if (inHorizontalBounds && abs(touchPx.y - topPx) <= edgeBandPx) return HandleType.TOP
        if (inHorizontalBounds && abs(touchPx.y - bottomPx) <= edgeBandPx) return HandleType.BOTTOM
        if (inVerticalBounds && abs(touchPx.x - leftPx) <= edgeBandPx) return HandleType.LEFT
        if (inVerticalBounds && abs(touchPx.x - rightPx) <= edgeBandPx) return HandleType.RIGHT

        // Priority 3: Check Box Interior
        if (touchPx.x >= leftPx && touchPx.x <= rightPx && touchPx.y >= topPx && touchPx.y <= bottomPx) {
            return HandleType.CENTER
        }

        // Priority 4: Background Empty Area
        return null
    }
}
