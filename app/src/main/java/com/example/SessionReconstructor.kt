package com.example

import android.app.usage.UsageEvents
import java.util.Date

data class Session(
    val appName: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long
)

class SessionReconstructor {
    fun reconstruct(events: List<UsageEvents.Event>): List<Session> {
        val sessions = mutableListOf<Session>()
        var currentApp: String? = null
        var currentStartTime: Long? = null

        val sortedEvents = events.sortedBy { it.timeStamp }

        for (event in sortedEvents) {
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (currentApp != null && currentStartTime != null) {
                        val endTime = event.timeStamp
                        val duration = endTime - currentStartTime!!
                        if (duration > 0) {
                            sessions.add(
                                Session(
                                    appName = currentApp!!,
                                    startTime = currentStartTime!!,
                                    endTime = endTime,
                                    duration = duration
                                )
                            )
                        }
                    }

                    currentApp = event.packageName
                    currentStartTime = event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.DEVICE_SHUTDOWN,
                UsageEvents.Event.USER_INTERACTION,
                UsageEvents.Event.SCREEN_INTERACTIVE,
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (currentApp != null && currentStartTime != null) {
                        val endTime = event.timeStamp
                        val duration = endTime - currentStartTime!!
                        if (duration > 0) {
                            sessions.add(
                                Session(
                                    appName = currentApp!!,
                                    startTime = currentStartTime!!,
                                    endTime = endTime,
                                    duration = duration
                                )
                            )
                        }
                        currentApp = null
                        currentStartTime = null
                    }
                }
            }
        }

        if (currentApp != null && currentStartTime != null) {
            val endTime = Date().time
            val duration = endTime - currentStartTime!!
            if (duration > 0) {
                sessions.add(
                    Session(
                        appName = currentApp!!,
                        startTime = currentStartTime!!,
                        endTime = endTime,
                        duration = duration
                    )
                )
            }
        }

        return sessions
    }
}
