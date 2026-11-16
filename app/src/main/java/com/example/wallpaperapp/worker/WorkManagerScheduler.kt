package com.example.wallpaperapp.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wallpaperapp.domain.model.ScheduleConfig
import com.example.wallpaperapp.domain.scheduler.WallpaperScheduler
import java.util.concurrent.TimeUnit

class WorkManagerScheduler(
    context: Context
) : WallpaperScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun schedule(config: ScheduleConfig) {
        if (config.isEnabled) {
            val intervalMinutes = config.intervalMinutes.coerceAtLeast(MIN_PERIODIC_INTERVAL_MINUTES)
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } else {
            cancel()
        }
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "periodic_wallpaper_rotation_work"
        const val MIN_PERIODIC_INTERVAL_MINUTES = 15L
    }
}
