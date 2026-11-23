package com.example.wallpaperapp.domain.repository

import com.example.wallpaperapp.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    suspend fun getAllQuotes(): List<Quote>
    suspend fun getQuoteById(id: String): Quote?
    suspend fun getRandomQuote(): Quote?
    suspend fun addQuote(quote: Quote)
    suspend fun addQuotes(quotes: List<Quote>)
    suspend fun updateQuote(quote: Quote)
    suspend fun deleteQuote(id: String)
    fun observeAllQuotes(): Flow<List<Quote>>
}
