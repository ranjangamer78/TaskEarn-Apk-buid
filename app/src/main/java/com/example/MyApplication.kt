package com.example

import android.app.Application
import android.util.Log
import com.example.data.repository.PreferencesManager

class MyApplication : Application() {
    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d("MyApplication", "Application started successfully")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
