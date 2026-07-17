package com.example

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncWorkManager {
    
    private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
    private const val ONE_TIME_SYNC_WORK_NAME = "one_time_sync_work"
    private const val PERIODIC_SYNC_INTERVAL_MINUTES = 15L
    
    /**
     * Schedule periodic sync worker (every 15 minutes)
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false) // Optional: set to true if you want to skip sync when battery is low
            .build()
        
        val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(
            PERIODIC_SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            periodicWorkRequest
        )
        
        Log.d("SyncWorkManager", "Periodic sync scheduled (every 15 minutes)")
    }
    
    /**
     * Trigger immediate one-time sync
     * Uses REPLACE policy to ensure only one sync runs at a time
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<OneTimeSyncWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace any existing work to avoid duplicates
            oneTimeWorkRequest
        )
        
        Log.d("SyncWorkManager", "Immediate sync triggered")
    }
    
    /**
     * Cancel all sync work
     */
    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_SYNC_WORK_NAME)
        Log.d("SyncWorkManager", "All sync work cancelled")
    }
}
