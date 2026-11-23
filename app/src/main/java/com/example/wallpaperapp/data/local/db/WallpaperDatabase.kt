package com.example.wallpaperapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [QuoteEntity::class, ImageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WallpaperDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun imageDao(): ImageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quotes ADD COLUMN showQuotationMarks INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
