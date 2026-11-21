package com.example.wallpaperapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleConfigTest {

    @Test
    fun `default ScheduleConfig is disabled with 60 minutes interval`() {
        val config = ScheduleConfig()
        assertFalse(config.isEnabled)
        assertEquals(60L, config.intervalMinutes)
    }

    @Test
    fun `enabled ScheduleConfig accepts 15 minutes or higher`() {
        val config = ScheduleConfig(isEnabled = true, intervalMinutes = 15L)
        assertTrue(config.isEnabled)
        assertEquals(15L, config.intervalMinutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `enabled ScheduleConfig rejects interval below 15 minutes`() {
        ScheduleConfig(isEnabled = true, intervalMinutes = 10L)
    }
}
