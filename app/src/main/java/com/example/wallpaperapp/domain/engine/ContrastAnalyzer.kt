package com.example.wallpaperapp.domain.engine

import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.TextColorScheme

data class ContrastResult(
    val textColorArgb: Int,
    val shadowColorArgb: Int,
    val isDarkBackground: Boolean
)

interface ContrastAnalyzer<I> {
    suspend fun analyzeContrast(
        image: I,
        rect: NormalizedRect,
        scheme: TextColorScheme = TextColorScheme.HIGH_CONTRAST
    ): ContrastResult
}
