package com.example.wallpaperapp.domain.engine

import com.example.wallpaperapp.domain.model.NormalizedRect

data class ContrastResult(
    val textColorArgb: Int,
    val shadowColorArgb: Int,
    val isDarkBackground: Boolean
)

interface ContrastAnalyzer<I> {
    /**
     * Analyzes luminance of the region defined by [rect] in [image]
     * and returns optimal text color and shadow color.
     */
    suspend fun analyzeContrast(image: I, rect: NormalizedRect): ContrastResult
}
