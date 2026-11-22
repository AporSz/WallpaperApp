package com.example.wallpaperapp.graphics

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.wallpaperapp.domain.applier.WallpaperApplier
import com.example.wallpaperapp.domain.model.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidWallpaperApplier(
    private val context: Context
) : WallpaperApplier<Bitmap> {

    override suspend fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val flags = when (target) {
                    WallpaperTarget.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(bitmap, null, true, flags)
                true
            } catch (e: Exception) {
                Log.e("AndroidWallpaperApplier", "Failed to set bitmap wallpaper to target: $target", e)
                false
            }
        }
    }
}
