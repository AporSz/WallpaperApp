package com.example.wallpaperapp.domain.model

data class ExportSummary(
    val exportedQuotesCount: Int,
    val exportedImagesCount: Int,
    val skippedImagesCount: Int
)
