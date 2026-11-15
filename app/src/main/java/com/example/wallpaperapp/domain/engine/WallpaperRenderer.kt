package com.example.wallpaperapp.domain.engine

import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.RenderConfig

interface WallpaperRenderer<I, B> {
    /**
     * Composites background [imageInput] with [quote] using [config].
     * Returns rendered output [B] (e.g. Bitmap).
     */
    suspend fun renderWallpaper(
        imageInput: I,
        quote: Quote,
        config: RenderConfig
    ): B?
}
