package com.example

import androidx.room.Transaction

class RoomSessionRepository(
    private val sessionDao: SessionDao,
    private val trackerMetadataDao: TrackerMetadataDao,
    private val sessionMapper: SessionMapper
) : SessionRepository {

    @Transaction
    override suspend fun saveSessions(
        sessions: List<Session>,
        lastProcessedEventTimestamp: Long
    ) {
        if (sessions.isNotEmpty()) {
            val entities = sessions.map(sessionMapper::toEntity)
            sessionDao.insertSessions(entities)
        }

        trackerMetadataDao.setValue(
            TrackerMetadataEntity(
                key = LAST_PROCESSED_EVENT_KEY,
                value = lastProcessedEventTimestamp.toString()
            )
        )
    }

    override suspend fun getPendingSessions(limit: Int): List<Session> {
        return sessionDao.getPendingSessions(limit).map(sessionMapper::toDomain)
    }

    override suspend fun markSessionSent(id: String) {
        sessionDao.updateSyncStatus(id, SYNC_STATUS_SENT, null)
    }

    override suspend fun markSessionFailed(id: String, errorMessage: String) {
        sessionDao.updateSyncStatus(id, SYNC_STATUS_FAILED, errorMessage)
    }

    override suspend fun getLastProcessedEventTimestamp(): Long? {
        return trackerMetadataDao.getValue(LAST_PROCESSED_EVENT_KEY)?.toLongOrNull()
    }

    override suspend fun setLastProcessedEventTimestamp(timestamp: Long) {
        trackerMetadataDao.setValue(
            TrackerMetadataEntity(
                key = LAST_PROCESSED_EVENT_KEY,
                value = timestamp.toString()
            )
        )
    }

    override suspend fun getLastSuccessfulSync(): Long? {
        return trackerMetadataDao.getValue(LAST_SUCCESSFUL_SYNC_KEY)?.toLongOrNull()
    }

    override suspend fun setLastSuccessfulSync(timestamp: Long) {
        trackerMetadataDao.setValue(
            TrackerMetadataEntity(
                key = LAST_SUCCESSFUL_SYNC_KEY,
                value = timestamp.toString()
            )
        )
    }

    override suspend fun getSessionsBetween(start: Long, end: Long): List<Session> {
        return sessionDao.getSessionsBetween(start, end).map(sessionMapper::toDomain)
    }

    companion object {
        private const val LAST_PROCESSED_EVENT_KEY = "last_processed_event"
        private const val LAST_SUCCESSFUL_SYNC_KEY = "last_successful_sync"
        private const val SYNC_STATUS_SENT = "SENT"
        private const val SYNC_STATUS_FAILED = "FAILED"
    }
}
