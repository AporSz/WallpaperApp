package com.example.wallpaperapp.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wallpaperapp.domain.model.WallpaperImage

@Entity(tableName = "wallpaper_images")
data class ImageEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val dateAdded: Long = System.currentTimeMillis()
) {
    fun toDomain(): WallpaperImage = WallpaperImage(id = id, uri = uri)

    companion object {
        fun fromDomain(image: WallpaperImage): ImageEntity = ImageEntity(
            id = image.id,
            uri = image.uri
        )
    }
}
