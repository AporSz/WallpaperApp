package com.example.wallpaperapp.graphics

import com.example.wallpaperapp.domain.model.LandscapeHandlingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class LandscapeOrientationTransformationTest {

    @Test
    fun `cacheKey reflects landscape handling mode`() {
        val transformationCw = LandscapeOrientationTransformation(LandscapeHandlingMode.ROTATE_90_CW)
        assertEquals("landscape_orientation_ROTATE_90_CW", transformationCw.cacheKey)

        val transformationCcw = LandscapeOrientationTransformation(LandscapeHandlingMode.ROTATE_90_CCW)
        assertEquals("landscape_orientation_ROTATE_90_CCW", transformationCcw.cacheKey)

        val transformationCrop = LandscapeOrientationTransformation(LandscapeHandlingMode.CROP)
        assertEquals("landscape_orientation_CROP", transformationCrop.cacheKey)
    }
}
