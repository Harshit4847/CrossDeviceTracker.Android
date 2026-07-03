package com.example

import android.app.usage.UsageEvents
import java.time.Instant
import java.util.Date

enum class SyncStatus {
    PENDING,
    SENT,
    FAILED
}

data class Session(
    val id: String = "",
    val packageName: String,
    val appName: String? = null,
    val startTimeUtc: Instant,
    val endTimeUtc: Instant,
    val durationSeconds: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAtUtc: Instant = Instant.now()
)

class SessionReconstructor {
    fun reconstruct(events: List<UsageEvents.Event>): List<Session> {
        val sessions = mutableListOf<Session>()
        var currentPackage: String? = null
        var currentStartTime: Long? = null

        val sortedEvents = events.sortedBy { it.timeStamp }

        for (event in sortedEvents) {
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (currentPackage != null && currentStartTime != null) {
                        val endTime = event.timeStamp
                        val durationMillis = endTime - currentStartTime!!
                        if (durationMillis > 0) {
                            sessions.add(createSession(currentPackage!!, event.packageName, currentStartTime!!, endTime, durationMillis))
                        }
                    }

                    currentPackage = event.packageName
                    currentStartTime = event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.DEVICE_SHUTDOWN,
                UsageEvents.Event.USER_INTERACTION,
                UsageEvents.Event.SCREEN_INTERACTIVE,
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (currentPackage != null && currentStartTime != null) {
                        val endTime = event.timeStamp
                        val durationMillis = endTime - currentStartTime!!
                        if (durationMillis > 0) {
                            sessions.add(createSession(currentPackage!!, null, currentStartTime!!, endTime, durationMillis))
                        }
                        currentPackage = null
                        currentStartTime = null
                    }
                }
            }
        }

        if (currentPackage != null && currentStartTime != null) {
            val endTime = Date().time
            val durationMillis = endTime - currentStartTime!!
            if (durationMillis > 0) {
                sessions.add(createSession(currentPackage!!, null, currentStartTime!!, endTime, durationMillis))
            }
        }

        return sessions
    }

    private fun createSession(
        packageName: String,
        appName: String?,
        startTimeMillis: Long,
        endTimeMillis: Long,
        durationMillis: Long
    ): Session {
        val startTimeUtc = Instant.ofEpochMilli(startTimeMillis)
        val endTimeUtc = Instant.ofEpochMilli(endTimeMillis)
        val durationSeconds = maxOf(1L, durationMillis / 1_000L)

        return Session(
            id = SessionIdGenerator.generate(packageName, startTimeUtc, endTimeUtc),
            packageName = packageName,
            appName = appName,
            startTimeUtc = startTimeUtc,
            endTimeUtc = endTimeUtc,
            durationSeconds = durationSeconds
        )
    }
}
