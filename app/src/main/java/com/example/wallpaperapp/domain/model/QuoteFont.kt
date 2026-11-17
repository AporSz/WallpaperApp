package com.example.wallpaperapp.domain.model

enum class FontType {
    BUILTIN_SANS,
    BUILTIN_SERIF,
    BUILTIN_MONO,
    BUILTIN_CURSIVE,
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
        val DEFAULT_CURSIVE = QuoteFont("builtin_cursive", "Cursive / Script", FontType.BUILTIN_CURSIVE)

        val BUILTIN_FONTS = listOf(
            DEFAULT_SANS,
            DEFAULT_SERIF,
            DEFAULT_MONO,
            DEFAULT_CURSIVE
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
