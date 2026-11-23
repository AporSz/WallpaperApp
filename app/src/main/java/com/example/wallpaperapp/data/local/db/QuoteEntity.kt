package com.example.wallpaperapp.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wallpaperapp.domain.model.Quote

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val author: String?,
    val showQuotationMarks: Boolean = true
) {
    fun toDomain(): Quote = Quote(
        id = id,
        text = text,
        author = author,
        showQuotationMarks = showQuotationMarks
    )

    companion object {
        fun fromDomain(quote: Quote): QuoteEntity = QuoteEntity(
            id = quote.id,
            text = quote.text,
            author = quote.author,
            showQuotationMarks = quote.showQuotationMarks
        )
    }
}
