package com.example

import android.app.Application
import android.util.Log
import com.example.data.repository.PreferencesManager
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Safety: Global Uncaught Exception Handler to prevent silent auto-closing / crashing
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MyApplication", "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize Firebase safely
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            Log.d("MyApplication", "Firebase initialized successfully")
        } catch (e: Throwable) {
            Log.e("MyApplication", "FirebaseApp init warning: ${e.message}")
        }
    }
}
