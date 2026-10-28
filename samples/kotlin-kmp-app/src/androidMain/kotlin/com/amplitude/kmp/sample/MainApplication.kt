package com.amplitude.kmp.sample

import android.app.Application

class MainApplication : Application() {
    companion object {
        const val AMPLITUDE_API_KEY = BuildConfig.AMPLITUDE_API_KEY
    }

    override fun onCreate() {
        super.onCreate()

        // Store Android context for Amplitude initialization
        androidContext = applicationContext
    }
}
