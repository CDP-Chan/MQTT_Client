package com.CDP.mqtt_client

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import java.util.Locale

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            SettingsManager.init()
        } catch (e: Exception) {
            Log.e("MyApp", "SettingsManager init failed", e)
        }

        applySavedLanguage()
    }

    @Suppress("DEPRECATION")
    private fun applySavedLanguage() {
        val lang = SettingsManager.getLanguage()
        val locale = if (lang == "zh") Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
