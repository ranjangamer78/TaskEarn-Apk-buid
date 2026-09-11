package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val darkMode: StateFlow<Boolean> = _darkMode

    private val _pushNotifications = MutableStateFlow(prefs.getBoolean("push_notifications", true))
    val pushNotifications: StateFlow<Boolean> = _pushNotifications

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _darkMode.value = enabled
    }

    fun setPushNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("push_notifications", enabled).apply()
        _pushNotifications.value = enabled
    }
}
