package com.example

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

class InstallationIdStore(context: Context) {
    private val prefs = context.getSharedPreferences("installation_prefs", Context.MODE_PRIVATE)

    fun getOrCreateInstallationId(): String {
        val existingId = prefs.getString(KEY_INSTALLATION_ID, null)
        if (!existingId.isNullOrBlank()) {
            return existingId
        }

        val newId = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_INSTALLATION_ID, newId) }
        return newId
    }

    fun getInstallationId(): String? = prefs.getString(KEY_INSTALLATION_ID, null)

    fun clearInstallationId() {
        prefs.edit { remove(KEY_INSTALLATION_ID) }
    }

    companion object {
        private const val KEY_INSTALLATION_ID = "installation_id"
    }
}
