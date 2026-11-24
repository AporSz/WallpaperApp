package com.example.wallpaperapp.domain.model

enum class TextColorScheme(val displayName: String, val description: String) {
    HIGH_CONTRAST("High Contrast", "Crisp white / deep charcoal"),
    SOFT_CONTRAST("Soft Contrast", "Warm cream / slate charcoal"),
    WARM_AMBER("Warm Amber", "Golden ivory / deep amber"),
    COOL_FROST("Cool Frost", "Ice blue / mist blue"),
    SUBTLE_PALETTE_HARMONY("Subtle Palette Harmony", "Soft, muted photo accent tones"),
    VIBRANT_PALETTE_HARMONY("Vibrant Palette Harmony", "Rich, colorful photo accent tones"),
    BLEND_IN("Blend In", "Soft background tonal camouflage")
}
