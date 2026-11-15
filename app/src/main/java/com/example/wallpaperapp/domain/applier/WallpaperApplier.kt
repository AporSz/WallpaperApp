package com.example.wallpaperapp.domain.applier

import com.example.wallpaperapp.domain.model.WallpaperTarget

interface WallpaperApplier<B> {
    /**
     * Applies [bitmap] to the specified [target] screen.
     */
    suspend fun applyWallpaper(bitmap: B, target: WallpaperTarget): Boolean
}
