package com.example

import android.content.Context
import androidx.core.content.edit

class DeviceTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)

    fun saveDeviceToken(token: String) {
        prefs.edit { putString(KEY_DEVICE_TOKEN, token) }
    }

    fun getDeviceToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)

    fun clearDeviceToken() {
        prefs.edit { remove(KEY_DEVICE_TOKEN) }
    }

    companion object {
        private const val KEY_DEVICE_TOKEN = "device_token"
    }
}
