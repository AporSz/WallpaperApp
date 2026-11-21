package com.example.wallpaperapp.domain.model

data class ScheduleConfig(
    val isEnabled: Boolean = false,
    val intervalMinutes: Long = 60L
) {
    init {
        require(!isEnabled || intervalMinutes >= MIN_INTERVAL_MINUTES) {
            "Schedule interval must be at least $MIN_INTERVAL_MINUTES minutes when enabled."
        }
    }

    companion object {
        const val MIN_INTERVAL_MINUTES = 15L
    }
}
