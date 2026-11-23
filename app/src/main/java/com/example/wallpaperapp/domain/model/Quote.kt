package com.example.wallpaperapp.domain.model

data class Quote(
    val id: String,
    val text: String,
    val author: String? = null,
    val showQuotationMarks: Boolean = true
) {
    init {
        require(text.isNotBlank()) { "Quote text cannot be blank" }
    }
}
