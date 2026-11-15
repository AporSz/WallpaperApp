package com.example.wallpaperapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [QuoteEntity::class, ImageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WallpaperDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun imageDao(): ImageDao
}
