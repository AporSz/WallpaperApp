package com.example.wallpaperapp.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.wallpaperapp.data.engine.ShuffleBagEngine
import com.example.wallpaperapp.data.local.db.WallpaperDatabase
import com.example.wallpaperapp.data.repository.DataStoreSettingsRepository
import com.example.wallpaperapp.data.repository.RoomImageRepository
import com.example.wallpaperapp.data.repository.RoomQuoteRepository
import com.example.wallpaperapp.data.repository.ZipLibraryBackupRepository
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.QuoteFont
import com.example.wallpaperapp.domain.model.WallpaperImage
import com.example.wallpaperapp.domain.repository.ImageRepository
import com.example.wallpaperapp.domain.repository.LibraryBackupRepository
import com.example.wallpaperapp.domain.repository.QuoteRepository
import com.example.wallpaperapp.domain.repository.SettingsRepository
import com.example.wallpaperapp.domain.scheduler.WallpaperScheduler
import com.example.wallpaperapp.domain.usecase.RotateWallpaperUseCase
import com.example.wallpaperapp.graphics.AndroidWallpaperApplier
import com.example.wallpaperapp.graphics.CanvasWallpaperRenderer
import com.example.wallpaperapp.graphics.PaletteContrastAnalyzer
import com.example.wallpaperapp.worker.WorkManagerScheduler

private val Context.dataStore by preferencesDataStore(name = "wallpaper_settings")

class AppContainer(private val context: Context) {

    val database: WallpaperDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            WallpaperDatabase::class.java,
            "wallpaper_db"
        )
            .addMigrations(WallpaperDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    val quoteRepository: QuoteRepository by lazy {
        RoomQuoteRepository(database.quoteDao())
    }

    val imageRepository: ImageRepository by lazy {
        RoomImageRepository(database.imageDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(context.applicationContext.dataStore)
    }

    val libraryBackupRepository: LibraryBackupRepository by lazy {
        ZipLibraryBackupRepository(
            context = context.applicationContext,
            quoteRepository = quoteRepository,
            imageRepository = imageRepository
        )
    }

    val quoteShuffleEngine = ShuffleBagEngine<Quote>()
    val imageShuffleEngine = ShuffleBagEngine<WallpaperImage>()
    val fontShuffleEngine = ShuffleBagEngine<QuoteFont>()

    val contrastAnalyzer by lazy {
        PaletteContrastAnalyzer()
    }

    val wallpaperRenderer by lazy {
        CanvasWallpaperRenderer(context.applicationContext, contrastAnalyzer)
    }

    val wallpaperApplier by lazy {
        AndroidWallpaperApplier(context.applicationContext)
    }

    val rotateWallpaperUseCase by lazy {
        RotateWallpaperUseCase(
            quoteRepository = quoteRepository,
            imageRepository = imageRepository,
            settingsRepository = settingsRepository,
            quoteShuffleEngine = quoteShuffleEngine,
            imageShuffleEngine = imageShuffleEngine,
            wallpaperRenderer = wallpaperRenderer,
            wallpaperApplier = wallpaperApplier,
            fontShuffleEngine = fontShuffleEngine
        )
    }

    val wallpaperScheduler: WallpaperScheduler by lazy {
        WorkManagerScheduler(context.applicationContext)
    }
}
