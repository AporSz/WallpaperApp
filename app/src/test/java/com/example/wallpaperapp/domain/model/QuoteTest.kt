package com.example.wallpaperapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteTest {

    @Test
    fun quote_defaultQuotationMarks_isEnabled() {
        val quote = Quote(id = "1", text = "Stay hungry, stay foolish", author = "Steve Jobs")
        assertTrue(quote.showQuotationMarks)
        assertEquals("1", quote.id)
        assertEquals("Stay hungry, stay foolish", quote.text)
        assertEquals("Steve Jobs", quote.author)
    }

    @Test
    fun quote_customQuotationMarks_isSet() {
        val quote = Quote(id = "2", text = "Carpe diem", author = "Horace", showQuotationMarks = false)
        assertFalse(quote.showQuotationMarks)
    }

    @Test(expected = IllegalArgumentException::class)
    fun quote_blankText_throwsException() {
        Quote(id = "3", text = "   ")
    }
}
