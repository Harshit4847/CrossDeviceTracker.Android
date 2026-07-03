package com.example

interface SessionRepository {
    suspend fun saveSessions(
        sessions: List<Session>,
        lastProcessedEventTimestamp: Long
    )

    suspend fun getPendingSessions(limit: Int): List<Session>

    suspend fun markSessionSent(id: String)

    suspend fun markSessionFailed(id: String, errorMessage: String)

    suspend fun getLastProcessedEventTimestamp(): Long?

    suspend fun setLastProcessedEventTimestamp(timestamp: Long)

    suspend fun getLastSuccessfulSync(): Long?

    suspend fun setLastSuccessfulSync(timestamp: Long)

    suspend fun getSessionsBetween(start: Long, end: Long): List<Session>
}
