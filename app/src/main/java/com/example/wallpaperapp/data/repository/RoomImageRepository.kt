package com.example.wallpaperapp.data.repository

import com.example.wallpaperapp.data.local.db.ImageDao
import com.example.wallpaperapp.data.local.db.ImageEntity
import com.example.wallpaperapp.domain.model.WallpaperImage
import com.example.wallpaperapp.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomImageRepository(
    private val imageDao: ImageDao
) : ImageRepository {

    override suspend fun getAllImages(): List<WallpaperImage> {
        return imageDao.getAllImages().map { it.toDomain() }
    }

    override suspend fun getImageById(id: String): WallpaperImage? {
        return imageDao.getImageById(id)?.toDomain()
    }

    override suspend fun addImage(image: WallpaperImage) {
        imageDao.insertImage(ImageEntity.fromDomain(image))
    }

    override suspend fun addImages(images: List<WallpaperImage>) {
        imageDao.insertImages(images.map { ImageEntity.fromDomain(it) })
    }

    override suspend fun deleteImage(id: String) {
        imageDao.deleteImageById(id)
    }

    override fun observeAllImages(): Flow<List<WallpaperImage>> {
        return imageDao.observeAllImages().map { list ->
            list.map { it.toDomain() }
        }
    }
}
