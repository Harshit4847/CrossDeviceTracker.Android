package com.example

import android.util.Log

class SessionCaptureService(
    private val usageStatsReader: UsageStatsReader,
    private val sessionReconstructor: SessionReconstructor,
    private val sessionRepository: SessionRepository
) {
    suspend fun capture(context: android.content.Context) {
        Log.d("SessionCapture", "Capture started")
        val lastProcessedEventTimestamp = sessionRepository.getLastProcessedEventTimestamp()
        Log.d("SessionCapture", "Last processed: $lastProcessedEventTimestamp")
        val events = usageStatsReader.readUsageEvents(context, lastProcessedEventTimestamp)
        Log.d("SessionCapture", "Events read: ${events.size}")
        val sessions = sessionReconstructor.reconstruct(events)

        val latestTimestamp = events.lastOrNull()?.timeStamp ?: lastProcessedEventTimestamp ?: 0L
        Log.d("SessionCapture", "Saving latest timestamp: $latestTimestamp")
        sessionRepository.saveSessions(sessions, latestTimestamp)
    }
}
