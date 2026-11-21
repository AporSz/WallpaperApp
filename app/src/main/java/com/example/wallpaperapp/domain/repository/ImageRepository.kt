package com.example.wallpaperapp.domain.repository

import com.example.wallpaperapp.domain.model.WallpaperImage
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    suspend fun getAllImages(): List<WallpaperImage>
    suspend fun getImageById(id: String): WallpaperImage?
    suspend fun getRandomImage(): WallpaperImage?
    suspend fun addImage(image: WallpaperImage)
    suspend fun addImages(images: List<WallpaperImage>)
    suspend fun deleteImage(id: String)
    fun observeAllImages(): Flow<List<WallpaperImage>>
}
