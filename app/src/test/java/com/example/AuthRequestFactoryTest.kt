package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthRequestFactoryTest {
    @Test
    fun createLoginPayload_includesEmailAndPassword() {
        val payload = AuthRequestFactory.createLoginPayload("user@example.com", "SecurePassword123")

        assertEquals("user@example.com", payload.get("email").asString)
        assertEquals("SecurePassword123", payload.get("password").asString)
    }

    @Test
    fun isValidLoginInput_rejectsBlankFields() {
        assertFalse(AuthRequestFactory.isValidLoginInput("", ""))
        assertFalse(AuthRequestFactory.isValidLoginInput("user@example.com", ""))
    }
}
