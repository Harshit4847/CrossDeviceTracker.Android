package com.example

import org.json.JSONObject

object AuthRequestFactory {
    fun createLoginPayload(email: String, password: String): JSONObject {
        return JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }
    }

    fun isValidLoginInput(email: String, password: String): Boolean {
        return email.isNotBlank() && password.isNotBlank()
    }
}
