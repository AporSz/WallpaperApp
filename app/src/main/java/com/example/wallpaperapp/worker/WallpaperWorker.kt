package com.example.wallpaperapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wallpaperapp.WallpaperApplication

class WallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? WallpaperApplication
            ?: return Result.failure()

        val rotateWallpaperUseCase = app.container.rotateWallpaperUseCase
        val result = rotateWallpaperUseCase()

        return if (result.isSuccess) {
            Result.success()
        } else {
            val exception = result.exceptionOrNull()
            exception?.printStackTrace()
            Result.retry()
        }
    }
}
