package com.harshit.crossdevicetracker

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionReconstructorTest {
    @Test
    fun reconstructsSessionsWithPackageAndAppName() {
        val events = listOf(
            createEvent(1_000L, 1, "com.instagram.android"),
            createEvent(3_000L, 1, "com.google.android.youtube"),
            createEvent(5_000L, 16, "")
        )

        val sessions = SessionReconstructor().reconstructFromEventData(events)

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

    private fun createEvent(time: Long, type: Int, packageName: String): SessionUsageEvent {
        return SessionUsageEvent(
            timeStamp = time,
            eventType = type,
            packageName = packageName
        )
    }
}
