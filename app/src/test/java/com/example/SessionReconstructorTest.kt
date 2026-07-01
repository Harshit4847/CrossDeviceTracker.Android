package com.example

import android.app.usage.UsageEvents
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionReconstructorTest {
    @Test
    fun reconstructsSessionsFromForegroundAndScreenOffEvents() {
        val events = listOf(
            createEvent(10_00_00L, UsageEvents.Event.MOVE_TO_FOREGROUND, "com.instagram.android"),
            createEvent(10_30_00L, UsageEvents.Event.MOVE_TO_FOREGROUND, "com.google.android.youtube"),
            createEvent(11_00_00L, UsageEvents.Event.SCREEN_NON_INTERACTIVE, "")
        )

        val sessions = SessionReconstructor().reconstruct(events)

        assertEquals(2, sessions.size)
        assertEquals("com.instagram.android", sessions[0].appName)
        assertEquals("com.google.android.youtube", sessions[1].appName)
    }

    private fun createEvent(time: Long, type: Int, packageName: String): UsageEvents.Event {
        return UsageEvents.Event().apply {
            this.timeStamp = time
            this.eventType = type
            this.packageName = packageName
        }
    }
}
