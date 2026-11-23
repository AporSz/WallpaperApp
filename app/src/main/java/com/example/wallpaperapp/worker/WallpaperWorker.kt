package com.example.wallpaperapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wallpaperapp.WallpaperApplication

class WallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("WallpaperWorker", "Starting scheduled wallpaper rotation work...")

        val app = applicationContext as? WallpaperApplication
        if (app == null) {
            Log.e("WallpaperWorker", "Application instance is null; failing worker job")
            return Result.failure()
        }

        val rotateWallpaperUseCase = app.container.rotateWallpaperUseCase
        val result = rotateWallpaperUseCase()

        return if (result.isSuccess) {
            Log.d("WallpaperWorker", "Wallpaper rotation executed successfully")
            Result.success()
        } else {
            val exception = result.exceptionOrNull()
            Log.e("WallpaperWorker", "Wallpaper rotation failed", exception)
            when (exception) {
                is IllegalStateException -> {
                    // Permanent error (empty asset pool, invalid configuration) -> fail without infinite retries
                    Log.e("WallpaperWorker", "Permanent failure encountered: ${exception.message}")
                    Result.failure()
                }
                else -> {
                    // Transient error -> retry according to WorkManager backoff policy
                    Log.w("WallpaperWorker", "Transient error encountered; requesting WorkManager retry")
                    Result.retry()
                }
            }
        }
    }
}
