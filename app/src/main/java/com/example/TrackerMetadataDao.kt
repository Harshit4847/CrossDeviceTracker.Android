package com.example

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TrackerMetadataDao {
    @Query("SELECT value FROM tracker_metadata WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun setValue(metadata: TrackerMetadataEntity)

    @Query("DELETE FROM tracker_metadata WHERE `key` = :key")
    suspend fun delete(key: String)
}
