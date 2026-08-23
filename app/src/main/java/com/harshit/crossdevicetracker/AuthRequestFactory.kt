package com.example

import com.google.gson.JsonObject

object AuthRequestFactory {
    fun createLoginPayload(email: String, password: String): JsonObject {
        return JsonObject().apply {
            addProperty("email", email.trim())
            addProperty("password", password)
        }
    }

    fun isValidLoginInput(email: String, password: String): Boolean {
        return email.isNotBlank() && password.isNotBlank()
    }
}
