package com.example.wallpaperapp.domain.model

data class NormalizedRect(
    val left: Float = 0.1f,
    val top: Float = 0.3f,
    val right: Float = 0.9f,
    val bottom: Float = 0.7f
) {
    init {
        require(left in 0.0f..1.0f) { "left must be between 0.0 and 1.0" }
        require(top in 0.0f..1.0f) { "top must be between 0.0 and 1.0" }
        require(right in 0.0f..1.0f) { "right must be between 0.0 and 1.0" }
        require(bottom in 0.0f..1.0f) { "bottom must be between 0.0 and 1.0" }
        require(left < right) { "left must be less than right" }
        require(top < bottom) { "top must be less than bottom" }
    }

    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        val UPPER_THIRD = NormalizedRect(left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.35f)
        val CENTER = NormalizedRect(left = 0.1f, top = 0.325f, right = 0.9f, bottom = 0.675f)
        val LOWER_THIRD = NormalizedRect(left = 0.1f, top = 0.65f, right = 0.9f, bottom = 0.9f)
    }
}
