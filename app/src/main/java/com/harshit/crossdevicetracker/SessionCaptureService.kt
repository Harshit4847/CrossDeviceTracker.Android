package com.harshit.crossdevicetracker

import android.util.Log

class SessionCaptureService(
    private val usageStatsReader: UsageStatsReader,
    private val sessionReconstructor: SessionReconstructor,
    private val sessionRepository: SessionRepository
) {
    suspend fun capture(context: android.content.Context) {
        Log.d("SessionCapture", "capture() started")
        Log.d("SessionCapture", "Capture started")
        val lastProcessedEventTimestamp = sessionRepository.getLastProcessedEventTimestamp()
        Log.d("SessionCapture", "Last processed: $lastProcessedEventTimestamp")
        val events = usageStatsReader.readUsageEvents(context, lastProcessedEventTimestamp)
        Log.d("SessionCapture", "Events read = ${events.size}")
        Log.d("SessionCapture", "Events read: ${events.size}")
        val sessions = sessionReconstructor.reconstruct(events)
        Log.d("SessionCapture", "Sessions reconstructed = ${sessions.size}")

        val latestTimestamp = events.lastOrNull()?.timeStamp ?: lastProcessedEventTimestamp ?: 0L
        Log.d("SessionCapture", "Saving latest timestamp: $latestTimestamp")
        Log.d("SessionCapture", "About to save sessions")
        sessionRepository.saveSessions(sessions, latestTimestamp)
    }
}
