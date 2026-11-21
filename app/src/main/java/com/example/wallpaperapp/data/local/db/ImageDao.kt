package com.example.wallpaperapp.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM wallpaper_images ORDER BY dateAdded DESC")
    suspend fun getAllImages(): List<ImageEntity>

    @Query("SELECT * FROM wallpaper_images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: String): ImageEntity?

    @Query("SELECT * FROM wallpaper_images ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomImage(): ImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("DELETE FROM wallpaper_images WHERE id = :id")
    suspend fun deleteImageById(id: String)

    @Query("SELECT * FROM wallpaper_images ORDER BY dateAdded DESC")
    fun observeAllImages(): Flow<List<ImageEntity>>
}
