package com.example.wallpaperapp

import android.app.Application
import com.example.wallpaperapp.di.AppContainer

class WallpaperApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
