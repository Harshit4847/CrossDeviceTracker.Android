package com.example

class SessionCaptureService(
    private val usageStatsReader: UsageStatsReader,
    private val sessionReconstructor: SessionReconstructor,
    private val sessionRepository: SessionRepository
) {
    suspend fun capture() {
        val lastProcessedEventTimestamp = sessionRepository.getLastProcessedEventTimestamp()
        val events = usageStatsReader.readUsageEvents(lastProcessedEventTimestamp)
        val sessions = sessionReconstructor.reconstruct(events)

        val latestTimestamp = events.lastOrNull()?.timeStamp ?: lastProcessedEventTimestamp ?: 0L
        sessionRepository.saveSessions(sessions, latestTimestamp)
    }
}
