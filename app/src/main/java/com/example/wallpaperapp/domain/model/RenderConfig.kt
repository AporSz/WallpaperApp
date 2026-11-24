package com.example.wallpaperapp.domain.model

data class RenderConfig(
    val targetRect: NormalizedRect = NormalizedRect.CENTER,
    val maxFontSizeSp: Float = 36.0f,
    val minFontSizeSp: Float = 12.0f,
    val textOrientation: TextOrientation = TextOrientation.HORIZONTAL,
    val fontId: String = "builtin_sans",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val landscapeHandlingMode: LandscapeHandlingMode = LandscapeHandlingMode.ROTATE_90_CW,
    val textColorScheme: TextColorScheme = TextColorScheme.HIGH_CONTRAST,
    val textGlowEnabled: Boolean = false
)
