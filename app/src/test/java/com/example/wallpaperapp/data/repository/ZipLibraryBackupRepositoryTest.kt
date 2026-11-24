package com.example.wallpaperapp.data.repository

import com.example.wallpaperapp.domain.model.BackupScope
import com.example.wallpaperapp.domain.model.ExportSummary
import com.example.wallpaperapp.domain.model.ImportSummary
import com.example.wallpaperapp.domain.model.Quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipLibraryBackupRepositoryTest {

    @Test
    fun backupScope_displayNames_areCorrect() {
        assertEquals("Both (Quotes & Images)", BackupScope.BOTH.displayName)
        assertEquals("Quotes Only", BackupScope.QUOTES_ONLY.displayName)
        assertEquals("Images Only", BackupScope.IMAGES_ONLY.displayName)
    }

    @Test
    fun exportSummary_fields_areCorrect() {
        val summary = ExportSummary(exportedQuotesCount = 10, exportedImagesCount = 5, skippedImagesCount = 1)
        assertEquals(10, summary.exportedQuotesCount)
        assertEquals(5, summary.exportedImagesCount)
        assertEquals(1, summary.skippedImagesCount)
    }

    @Test
    fun importSummary_fields_areCorrect() {
        val summary = ImportSummary(
            importedQuotesCount = 8,
            skippedDuplicateQuotesCount = 2,
            importedImagesCount = 4,
            skippedImagesCount = 0
        )
        assertEquals(8, summary.importedQuotesCount)
        assertEquals(2, summary.skippedDuplicateQuotesCount)
        assertEquals(4, summary.importedImagesCount)
        assertEquals(0, summary.skippedImagesCount)
    }

    @Test
    fun jsonBackupHelper_serializeAndParse_roundTripPreservesFields() {
        val originalQuotes = listOf(
            Quote(id = "q1", text = "Carpe diem", author = "Horace", showQuotationMarks = false),
            Quote(id = "q2", text = "Stay hungry, stay foolish", author = "Steve Jobs", showQuotationMarks = true)
        )

        val jsonString = JsonBackupHelper.serializeQuotes(originalQuotes)
        assertTrue(jsonString.contains("Carpe diem"))
        assertTrue(jsonString.contains("Horace"))

        val parsedQuotes = JsonBackupHelper.parseQuotes(jsonString)
        assertEquals(2, parsedQuotes.size)

        val q1 = parsedQuotes.find { it.text == "Carpe diem" }!!
        assertEquals("q1", q1.id)
        assertEquals("Horace", q1.author)
        assertFalse(q1.showQuotationMarks)

        val q2 = parsedQuotes.find { it.text == "Stay hungry, stay foolish" }!!
        assertEquals("q2", q2.id)
        assertEquals("Steve Jobs", q2.author)
        assertTrue(q2.showQuotationMarks)
    }
}
