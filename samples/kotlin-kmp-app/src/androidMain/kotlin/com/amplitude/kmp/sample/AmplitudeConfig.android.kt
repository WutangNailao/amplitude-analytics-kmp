package com.amplitude.kmp.sample

import android.annotation.SuppressLint
import android.content.Context

/**
 * Android-specific Amplitude API key.
 */
actual val amplitudeApiKey: String
    get() = androidApiKey

private var androidApiKey: String = ""

/**
 * Android application context.
 * Set by MainApplication.
 */
@SuppressLint("StaticFieldLeak")
private var androidContext: Context? = null

/** Configure the shared sample before displaying its UI. Only retain the application context. */
fun configureAndroidSample(
    context: Context,
    apiKey: String,
) {
    androidContext = context.applicationContext
    androidApiKey = apiKey
}

actual val platformContext: Any?
    get() = androidContext
