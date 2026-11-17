package com.example.wallpaperapp.domain.model

enum class FontType {
    BUILTIN_SANS,
    BUILTIN_SERIF,
    BUILTIN_MONO,
    BUILTIN_CURSIVE,
    BUILTIN_CASUAL,
    BUILTIN_SANS_MEDIUM,
    BUILTIN_SERIF_BOLD,
    BUILTIN_MONO_MEDIUM,
    BUILTIN_SANS_BLACK,
    BUILTIN_SANS_CONDENSED,
    BUILTIN_SANS_LIGHT,
    BUILTIN_SERIF_ITALIC,
    CUSTOM_FILE
}

data class QuoteFont(
    val id: String,
    val name: String,
    val type: FontType,
    val customFilePath: String? = null
) {
    companion object {
        val DEFAULT_SANS = QuoteFont("builtin_sans", "Sans Serif", FontType.BUILTIN_SANS)
        val DEFAULT_SERIF = QuoteFont("builtin_serif", "Serif", FontType.BUILTIN_SERIF)
        val DEFAULT_MONO = QuoteFont("builtin_mono", "Monospace", FontType.BUILTIN_MONO)
        val DEFAULT_CURSIVE = QuoteFont("builtin_cursive", "Cursive Script", FontType.BUILTIN_CURSIVE)
        val CASUAL = QuoteFont("builtin_casual", "Casual Hand", FontType.BUILTIN_CASUAL)
        val SANS_MEDIUM = QuoteFont("builtin_sans_medium", "Sans Medium", FontType.BUILTIN_SANS_MEDIUM)
        val SERIF_BOLD = QuoteFont("builtin_serif_bold", "Headline Serif", FontType.BUILTIN_SERIF_BOLD)
        val MONO_MEDIUM = QuoteFont("builtin_mono_medium", "Tech Mono", FontType.BUILTIN_MONO_MEDIUM)
        val SANS_BLACK = QuoteFont("builtin_sans_black", "Poster Sans", FontType.BUILTIN_SANS_BLACK)
        val SANS_CONDENSED = QuoteFont("builtin_sans_condensed", "Condensed Display", FontType.BUILTIN_SANS_CONDENSED)
        val SANS_LIGHT = QuoteFont("builtin_sans_light", "Elegant Light", FontType.BUILTIN_SANS_LIGHT)
        val SERIF_ITALIC = QuoteFont("builtin_serif_italic", "Classic Editorial", FontType.BUILTIN_SERIF_ITALIC)

        val BUILTIN_FONTS = listOf(
            DEFAULT_SANS,
            DEFAULT_SERIF,
            DEFAULT_MONO,
            DEFAULT_CURSIVE,
            CASUAL,
            SANS_MEDIUM,
            SERIF_BOLD,
            MONO_MEDIUM,
            SANS_BLACK,
            SANS_CONDENSED,
            SANS_LIGHT,
            SERIF_ITALIC
        )

        fun findById(id: String): QuoteFont {
            return BUILTIN_FONTS.find { it.id == id }
                ?: if (id.startsWith("custom_")) {
                    QuoteFont(id, "Custom Font", FontType.CUSTOM_FILE, customFilePath = id.removePrefix("custom_"))
                } else {
                    DEFAULT_SANS
                }
        }
    }
}
