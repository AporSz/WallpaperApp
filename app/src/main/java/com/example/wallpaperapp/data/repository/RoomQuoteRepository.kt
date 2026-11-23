package com.example.wallpaperapp.data.repository

import com.example.wallpaperapp.data.local.db.QuoteDao
import com.example.wallpaperapp.data.local.db.QuoteEntity
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomQuoteRepository(
    private val quoteDao: QuoteDao
) : QuoteRepository {

    override suspend fun getAllQuotes(): List<Quote> {
        return quoteDao.getAllQuotes().map { it.toDomain() }
    }

    override suspend fun getQuoteById(id: String): Quote? {
        return quoteDao.getQuoteById(id)?.toDomain()
    }

    override suspend fun getRandomQuote(): Quote? {
        return quoteDao.getRandomQuote()?.toDomain()
    }

    override suspend fun addQuote(quote: Quote) {
        quoteDao.insertQuote(QuoteEntity.fromDomain(quote))
    }

    override suspend fun addQuotes(quotes: List<Quote>) {
        quoteDao.insertQuotes(quotes.map { QuoteEntity.fromDomain(it) })
    }

    override suspend fun updateQuote(quote: Quote) {
        quoteDao.updateQuote(QuoteEntity.fromDomain(quote))
    }

    override suspend fun deleteQuote(id: String) {
        quoteDao.deleteQuoteById(id)
    }

    override fun observeAllQuotes(): Flow<List<Quote>> {
        return quoteDao.observeAllQuotes().map { list ->
            list.map { it.toDomain() }
        }
    }
}
