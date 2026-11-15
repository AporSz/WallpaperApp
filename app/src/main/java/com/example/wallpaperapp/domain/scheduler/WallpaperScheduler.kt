package com.example.wallpaperapp.domain.scheduler

import com.example.wallpaperapp.domain.model.ScheduleConfig

interface WallpaperScheduler {
    /**
     * Schedules periodic wallpaper updates with the given [config].
     */
    fun schedule(config: ScheduleConfig)

    /**
     * Cancels any scheduled wallpaper updates.
     */
    fun cancel()
}
