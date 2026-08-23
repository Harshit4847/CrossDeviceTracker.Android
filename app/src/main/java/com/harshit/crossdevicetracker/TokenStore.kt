package com.example

import android.content.Context
import androidx.core.content.edit

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit { putString(KEY_ACCESS_TOKEN, token) }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun clearToken() {
        prefs.edit { remove(KEY_ACCESS_TOKEN) }
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
