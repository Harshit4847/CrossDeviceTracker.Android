package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionCaptureServiceTest {
    private lateinit var database: AppDatabase
    private lateinit var service: SessionCaptureService

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
        UsageStatsReader.eventProvider = null
    }

    @Test
    fun capture_persistsSessionsAndCheckpointAndPreventsDuplicates() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        val repository = RoomSessionRepository(
            database.sessionDao(),
            database.trackerMetadataDao(),
            SessionMapper
        )
        service = SessionCaptureService(
            usageStatsReader = UsageStatsReader,
            sessionReconstructor = SessionReconstructor(),
            sessionRepository = repository
        )

        val events = listOf(
            createEvent(packageName = "com.instagram.android", eventType = 1, timestamp = 100L),
            createEvent(packageName = "com.instagram.android", eventType = 2, timestamp = 200L),
            createEvent(packageName = "com.youtube.android", eventType = 1, timestamp = 200L),
            createEvent(packageName = "com.youtube.android", eventType = 2, timestamp = 300L)
        )

        UsageStatsReader.eventProvider = { _ -> events }

        service.capture()

        val savedSessions = database.sessionDao().getSessionsBetween(0L, 1_000_000L)
        assertEquals(2, savedSessions.size)
        assertEquals("com.instagram.android", savedSessions.first { it.packageName == "com.instagram.android" }.packageName)
        assertEquals("com.youtube.android", savedSessions.first { it.packageName == "com.youtube.android" }.packageName)

        val checkpoint = database.trackerMetadataDao().getValue(TrackerCheckpointKeys.LAST_PROCESSED_EVENT)
        assertEquals("300", checkpoint)

        service.capture()

        val sessionsAfterSecondCapture = database.sessionDao().getSessionsBetween(0L, 1_000_000L)
        assertEquals(2, sessionsAfterSecondCapture.size)

        val storedCheckpoint = database.trackerMetadataDao().getValue(TrackerCheckpointKeys.LAST_PROCESSED_EVENT)
        assertEquals("300", storedCheckpoint)
    }

    private fun createEvent(packageName: String, eventType: Int, timestamp: Long): UsageEvents.Event {
        val event = UsageEvents.Event()
        event.packageName = packageName
        event.eventType = eventType
        event.timeStamp = timestamp
        return event
    }
}
