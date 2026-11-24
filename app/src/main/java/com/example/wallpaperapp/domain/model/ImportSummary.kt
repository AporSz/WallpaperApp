package com.example.wallpaperapp.domain.model

data class ImportSummary(
    val importedQuotesCount: Int,
    val skippedDuplicateQuotesCount: Int,
    val importedImagesCount: Int,
    val skippedImagesCount: Int
)
