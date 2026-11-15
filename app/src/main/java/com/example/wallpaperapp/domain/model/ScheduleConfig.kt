package com.example.wallpaperapp.domain.model

data class ScheduleConfig(
    val isEnabled: Boolean = false,
    val intervalMinutes: Long = 60L
)
