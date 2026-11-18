package com.example.wallpaperapp.graphics

import android.graphics.Bitmap
import android.graphics.Matrix
import coil.size.Size
import coil.transform.Transformation
import com.example.wallpaperapp.domain.model.LandscapeHandlingMode

class LandscapeOrientationTransformation(
    private val mode: LandscapeHandlingMode
) : Transformation {

    override val cacheKey: String = "landscape_orientation_${mode.name}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val srcWidth = input.width
        val srcHeight = input.height

        // If source is landscape (width > height), apply landscape handling transformation off-main-thread
        if (srcWidth > srcHeight) {
            val rotationDegrees = when (mode) {
                LandscapeHandlingMode.ROTATE_90_CW -> 90f
                LandscapeHandlingMode.ROTATE_90_CCW -> -90f
                LandscapeHandlingMode.CROP -> 0f
            }

            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                return Bitmap.createBitmap(input, 0, 0, srcWidth, srcHeight, matrix, true)
            }
        }

        return input
    }
}
