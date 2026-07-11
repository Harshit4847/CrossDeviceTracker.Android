package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncManager(
    private val context: Context,
    private val sessionSyncService: SessionSyncService,
    private val deviceTokenStore: DeviceTokenStore,
    private val sessionRepository: SessionRepository
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var periodicSyncJob: Job? = null
    private var isSyncing = false
    private val SYNC_INTERVAL_MS = 30_000L // 30 seconds

    /**
     * Start periodic sync with 30-second interval
     */
    fun startPeriodicSync() {
        if (periodicSyncJob?.isActive == true) {
            Log.d("SyncManager", "Periodic sync already running")
            return
        }

        periodicSyncJob = syncScope.launch {
            Log.d("SyncManager", "Starting periodic sync (30-second interval)")
            
            // Initial sync on app start
            attemptSync("App Start")
            
            while (true) {
                delay(SYNC_INTERVAL_MS)
                attemptSync("Periodic Timer")
            }
        }
    }

    /**
     * Stop periodic sync
     */
    fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
        Log.d("SyncManager", "Periodic sync stopped")
    }

    /**
     * Observe network connectivity and trigger sync on reconnect
     */
    fun observeNetworkAndSync() {
        NetworkConnectivityManager.observeNetworkConnectivity(context)
            .onEach { isConnected ->
                if (isConnected) {
                    Log.d("SyncManager", "Network reconnected, triggering sync")
                    attemptSync("Network Reconnect")
                }
            }
            .launchIn(syncScope)
    }

    /**
     * Attempt to sync pending sessions with safety checks
     */
    private suspend fun attemptSync(trigger: String) {
        // Check if sync is already running
        if (isSyncing) {
            Log.d("SyncManager", "Sync already in progress, skipping ($trigger)")
            return
        }

        // Check if device JWT is available
        val deviceToken = deviceTokenStore.getDeviceToken()
        if (deviceToken.isNullOrBlank()) {
            Log.d("SyncManager", "Device JWT not available, skipping sync ($trigger)")
            return
        }

        // Check if internet is available
        if (!NetworkConnectivityManager.isInternetAvailable(context)) {
            Log.d("SyncManager", "Internet not available, skipping sync ($trigger)")
            return
        }

        // Check if there are pending sessions
        val pendingSessions = withContext(Dispatchers.IO) {
            sessionRepository.getPendingSessions(limit = 1)
        }
        if (pendingSessions.isEmpty()) {
            Log.d("SyncManager", "No pending sessions, skipping sync ($trigger)")
            return
        }

        // Perform sync
        isSyncing = true
        try {
            Log.d("SyncManager", "Starting sync (trigger: $trigger)")
            val result = sessionSyncService.syncPendingSessions()
            Log.d("SyncManager", "Sync completed: $result (trigger: $trigger)")
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed (trigger: $trigger)", e)
        } finally {
            isSyncing = false
        }
    }

    /**
     * Manually trigger sync (e.g., from "Sync Now" button)
     */
    suspend fun manualSync(): SessionSyncResult {
        if (isSyncing) {
            Log.d("SyncManager", "Manual sync requested but sync already in progress")
            return SessionSyncResult.SERVER_ERROR
        }

        val deviceToken = deviceTokenStore.getDeviceToken()
        if (deviceToken.isNullOrBlank()) {
            Log.d("SyncManager", "Manual sync failed: Device JWT not available")
            return SessionSyncResult.AUTH_ERROR
        }

        if (!NetworkConnectivityManager.isInternetAvailable(context)) {
            Log.d("SyncManager", "Manual sync failed: Internet not available")
            return SessionSyncResult.NETWORK_ERROR
        }

        isSyncing = true
        try {
            Log.d("SyncManager", "Starting manual sync")
            val result = sessionSyncService.syncPendingSessions()
            Log.d("SyncManager", "Manual sync completed: $result")
            return result
        } catch (e: Exception) {
            Log.e("SyncManager", "Manual sync failed", e)
            return SessionSyncResult.SERVER_ERROR
        } finally {
            isSyncing = false
        }
    }
}
