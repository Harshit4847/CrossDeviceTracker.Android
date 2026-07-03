package com.example

import java.security.MessageDigest
import java.time.Instant

object SessionIdGenerator {
    fun generate(packageName: String, startTimeUtc: Instant, endTimeUtc: Instant): String {
        val input = buildString {
            append(packageName)
            append('|')
            append(startTimeUtc.toEpochMilli())
            append('|')
            append(endTimeUtc.toEpochMilli())
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
