package com.example

import android.util.Log
import java.io.IOException
import java.time.Instant

class SessionSyncService(
    private val sessionRepository: SessionRepository,
    private val sessionApi: SessionApi,
    private val deviceTokenStore: DeviceTokenStore
) {
    suspend fun syncPendingSessions(): SessionSyncResult {
        val deviceToken = deviceTokenStore.getDeviceToken()
        if (deviceToken.isNullOrBlank()) {
            return SessionSyncResult.AUTH_ERROR
        }

        val authHeader = "Bearer $deviceToken"
        var hasSuccessfulBatch = false
        var hasFailedBatch = false
        var lastErrorResult: SessionSyncResult? = null

        while (true) {
            val pendingSessions = sessionRepository.getPendingSessions(limit = 50)
            if (pendingSessions.isEmpty()) {
                break
            }

            val dtos = pendingSessions.map(SessionUploadMapper::toDto)
            val result = try {
                val response = sessionApi.uploadSessions(authHeader, dtos)
                Log.d("API_RESPONSE", response.toString())
                if (response.isSuccessful) {
                    for (session in pendingSessions) {
                        sessionRepository.markSessionSent(session.id)
                    }
                    sessionRepository.setLastSuccessfulSync(Instant.now().toEpochMilli())
                    hasSuccessfulBatch = true
                    null
                } else {
                    val code = response.code()
                    if (code == 401) {
                        SessionSyncResult.AUTH_ERROR
                    } else if (code >= 500) {
                        val errMsg = "Server error: $code"
                        for (session in pendingSessions) {
                            sessionRepository.markSessionFailed(session.id, errMsg)
                        }
                        hasFailedBatch = true
                        SessionSyncResult.SERVER_ERROR
                    } else {
                        val errMsg = "API error: $code"
                        for (session in pendingSessions) {
                            sessionRepository.markSessionFailed(session.id, errMsg)
                        }
                        hasFailedBatch = true
                        SessionSyncResult.SERVER_ERROR
                    }
                }
            } catch (e: IOException) {
                SessionSyncResult.NETWORK_ERROR
            } catch (e: Exception) {
                val errMsg = "Unexpected error: ${e.message}"
                for (session in pendingSessions) {
                    sessionRepository.markSessionFailed(session.id, errMsg)
                }
                hasFailedBatch = true
                SessionSyncResult.SERVER_ERROR
            }

            if (result != null) {
                lastErrorResult = result
                if (result == SessionSyncResult.AUTH_ERROR || result == SessionSyncResult.NETWORK_ERROR) {
                    break
                }
            }
        }

        return when {
            lastErrorResult == SessionSyncResult.AUTH_ERROR -> SessionSyncResult.AUTH_ERROR
            lastErrorResult == SessionSyncResult.NETWORK_ERROR -> {
                if (hasSuccessfulBatch) SessionSyncResult.PARTIAL_SUCCESS else SessionSyncResult.NETWORK_ERROR
            }
            hasFailedBatch && hasSuccessfulBatch -> SessionSyncResult.PARTIAL_SUCCESS
            hasFailedBatch -> lastErrorResult ?: SessionSyncResult.SERVER_ERROR
            else -> SessionSyncResult.SUCCESS
        }
    }
}
