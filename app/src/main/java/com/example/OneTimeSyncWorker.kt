package com.example

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OneTimeSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("OneTimeSyncWorker", "Starting one-time sync")
            
            // Initialize dependencies
            val deviceTokenStore = DeviceTokenStore(applicationContext)
            val database = AppDatabase.getDatabase(applicationContext)
            val sessionRepository = RoomSessionRepository(
                database.sessionDao(),
                database.trackerMetadataDao(),
                SessionMapper
            )
            
            // Check Device JWT
            val deviceToken = deviceTokenStore.getDeviceToken()
            if (deviceToken.isNullOrBlank()) {
                Log.d("OneTimeSyncWorker", "Device JWT not available, skipping sync")
                return Result.failure()
            }
            
            // Check internet
            if (!NetworkConnectivityManager.isInternetAvailable(applicationContext)) {
                Log.d("OneTimeSyncWorker", "Internet not available, skipping sync")
                return Result.retry()
            }
            
            // Check pending sessions
            val pendingSessions = sessionRepository.getPendingSessions(limit = 100)
            if (pendingSessions.isEmpty()) {
                Log.d("OneTimeSyncWorker", "No pending sessions, skipping sync")
                return Result.success()
            }
            
            // Create SessionSyncService and sync
            val sessionApi = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SessionApi::class.java)
            
            val sessionSyncService = SessionSyncService(
                sessionRepository,
                sessionApi,
                deviceTokenStore
            )
            
            val result = sessionSyncService.syncPendingSessions()
            
            when (result) {
                SessionSyncResult.SUCCESS -> {
                    Log.d("OneTimeSyncWorker", "Sync completed successfully")
                    Result.success()
                }
                SessionSyncResult.AUTH_ERROR -> {
                    Log.d("OneTimeSyncWorker", "Auth error, skipping sync")
                    Result.failure()
                }
                SessionSyncResult.NETWORK_ERROR -> {
                    Log.d("OneTimeSyncWorker", "Network error, will retry")
                    Result.retry()
                }
                else -> {
                    Log.d("OneTimeSyncWorker", "Sync failed with unknown error")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e("OneTimeSyncWorker", "Sync failed with exception", e)
            Result.retry()
        }
    }
}
