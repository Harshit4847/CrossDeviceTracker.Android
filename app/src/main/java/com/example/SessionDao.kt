package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Query("SELECT * FROM sessions WHERE syncStatus = 'PENDING' ORDER BY startTimeUtc LIMIT :limit")
    suspend fun getPendingSessions(limit: Int): List<SessionEntity>

    @Query("UPDATE sessions SET syncStatus = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, errorMessage: String?): Int

    @Query("SELECT * FROM sessions WHERE startTimeUtc >= :startTime AND endTimeUtc <= :endTime ORDER BY startTimeUtc")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE syncStatus = 'SENT'")
    suspend fun deleteSyncedSessions(): Int
}
