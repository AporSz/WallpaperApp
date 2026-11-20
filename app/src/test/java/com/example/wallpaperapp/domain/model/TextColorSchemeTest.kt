package com.example.wallpaperapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TextColorSchemeTest {

    @Test
    fun `text color scheme entries contain 5 distinct options`() {
        val entries = TextColorScheme.entries
        assertEquals(5, entries.size)
        assertEquals(TextColorScheme.HIGH_CONTRAST, entries[0])
        assertEquals(TextColorScheme.SOFT_CONTRAST, entries[1])
        assertEquals(TextColorScheme.WARM_AMBER, entries[2])
        assertEquals(TextColorScheme.COOL_FROST, entries[3])
        assertEquals(TextColorScheme.PALETTE_HARMONY, entries[4])
    }
}
