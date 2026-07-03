package com.example

import android.app.usage.UsageEvents
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionReconstructorTest {
    @Test
    fun reconstructsSessionsWithPackageAndAppName() {
        val events = listOf(
            createEvent(1_000L, UsageEvents.Event.MOVE_TO_FOREGROUND, "com.instagram.android"),
            createEvent(3_000L, UsageEvents.Event.MOVE_TO_FOREGROUND, "com.google.android.youtube"),
            createEvent(5_000L, UsageEvents.Event.SCREEN_NON_INTERACTIVE, "")
        )

        val sessions = SessionReconstructor().reconstruct(events)

        assertEquals(2, sessions.size)

        assertEquals("com.instagram.android", sessions[0].packageName)
        assertEquals("com.instagram.android", sessions[0].appName)
        assertEquals(Instant.ofEpochMilli(1_000L), sessions[0].startTimeUtc)
        assertEquals(Instant.ofEpochMilli(3_000L), sessions[0].endTimeUtc)
        assertEquals(2L, sessions[0].durationSeconds)
        assertEquals(SyncStatus.PENDING, sessions[0].syncStatus)

        assertEquals("com.google.android.youtube", sessions[1].packageName)
        assertEquals("com.google.android.youtube", sessions[1].appName)
        assertEquals(Instant.ofEpochMilli(3_000L), sessions[1].startTimeUtc)
        assertEquals(Instant.ofEpochMilli(5_000L), sessions[1].endTimeUtc)
        assertEquals(2L, sessions[1].durationSeconds)
        assertEquals(SyncStatus.PENDING, sessions[1].syncStatus)
    }

    private fun createEvent(time: Long, type: Int, packageName: String): UsageEvents.Event {
        return UsageEvents.Event().apply {
            this.timeStamp = time
            this.eventType = type
            this.packageName = packageName
        }
    }
}
