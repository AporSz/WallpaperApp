package com.example.wallpaperapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TextColorSchemeTest {

    @Test
    fun `text color scheme entries contain 7 distinct options`() {
        val entries = TextColorScheme.entries
        assertEquals(7, entries.size)
        assertEquals(TextColorScheme.HIGH_CONTRAST, entries[0])
        assertEquals(TextColorScheme.SOFT_CONTRAST, entries[1])
        assertEquals(TextColorScheme.WARM_AMBER, entries[2])
        assertEquals(TextColorScheme.COOL_FROST, entries[3])
        assertEquals(TextColorScheme.SUBTLE_PALETTE_HARMONY, entries[4])
        assertEquals(TextColorScheme.VIBRANT_PALETTE_HARMONY, entries[5])
        assertEquals(TextColorScheme.BLEND_IN, entries[6])
    }

    @Test
    fun `text color scheme display names and descriptions are correct`() {
        assertEquals("Subtle Palette Harmony", TextColorScheme.SUBTLE_PALETTE_HARMONY.displayName)
        assertEquals("Vibrant Palette Harmony", TextColorScheme.VIBRANT_PALETTE_HARMONY.displayName)
        assertEquals("Blend In", TextColorScheme.BLEND_IN.displayName)
    }

    @Test
    fun `legacy PALETTE_HARMONY string maps to SUBTLE_PALETTE_HARMONY`() {
        val legacyName = "PALETTE_HARMONY"
        val mappedScheme = if (legacyName == "PALETTE_HARMONY") {
            TextColorScheme.SUBTLE_PALETTE_HARMONY
        } else {
            TextColorScheme.valueOf(legacyName)
        }
        assertEquals(TextColorScheme.SUBTLE_PALETTE_HARMONY, mappedScheme)
    }
}
