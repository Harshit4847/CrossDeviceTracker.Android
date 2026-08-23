package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class SessionSyncServiceTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomSessionRepository
    private lateinit var tokenStore: DeviceTokenStore
    private lateinit var fakeApi: FakeSessionApi
    private lateinit var syncService: SessionSyncService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomSessionRepository(
            database.sessionDao(),
            database.trackerMetadataDao(),
            SessionMapper
        )
        tokenStore = DeviceTokenStore(context)
        tokenStore.saveDeviceToken("fake-jwt-token")
        fakeApi = FakeSessionApi()
        syncService = SessionSyncService(repository, fakeApi, tokenStore)
    }

    @After
    fun tearDown() {
        database.close()
        tokenStore.clearDeviceToken()
    }

    @Test
    fun sync_noPendingSessions_returnsSuccess() = runBlocking {
        val result = syncService.syncPendingSessions()
        assertEquals(SessionSyncResult.SUCCESS, result)
        assertEquals(0, fakeApi.uploadedBatches.size)
    }

    @Test
    fun sync_withPendingSessions_uploadsAndMarksSent() = runBlocking {
        val sessions = listOf(
            createSession("com.app.one", 1000L, 2000L),
            createSession("com.app.two", 2000L, 3000L)
        )
        repository.saveSessions(sessions, 3000L)

        val result = syncService.syncPendingSessions()

        assertEquals(SessionSyncResult.SUCCESS, result)
        assertEquals(1, fakeApi.uploadedBatches.size)
        assertEquals(2, fakeApi.uploadedBatches[0].size)
        assertEquals("Bearer fake-jwt-token", fakeApi.lastAuthHeader)

        // Verify database states
        val pending = repository.getPendingSessions(10)
        assertTrue(pending.isEmpty())

        val dbSessions = database.sessionDao().getSessionsBetween(0L, 10000L)
        assertEquals(2, dbSessions.size)
        assertTrue(dbSessions.all { it.syncStatus == "SENT" })

        // Verify last successful sync metadata
        val lastSync = repository.getLastSuccessfulSync()
        assertNotNull(lastSync)
    }

    @Test
    fun sync_batching_uploadsInGroupsOf50() = runBlocking {
        val sessions = mutableListOf<Session>()
        for (i in 1..120) {
            sessions.add(createSession("com.app.$i", i * 1000L, i * 1000L + 500L))
        }
        repository.saveSessions(sessions, 200000L)

        val result = syncService.syncPendingSessions()

        assertEquals(SessionSyncResult.SUCCESS, result)
        assertEquals(3, fakeApi.uploadedBatches.size)
        assertEquals(50, fakeApi.uploadedBatches[0].size)
        assertEquals(50, fakeApi.uploadedBatches[1].size)
        assertEquals(20, fakeApi.uploadedBatches[2].size)

        val pending = repository.getPendingSessions(200)
        assertTrue(pending.isEmpty())
    }

    @Test
    fun sync_authError_stopsSyncAndReturnsAuthError() = runBlocking {
        val sessions = listOf(
            createSession("com.app.one", 1000L, 2000L)
        )
        repository.saveSessions(sessions, 2000L)
        fakeApi.shouldFailWithCode = 401

        val result = syncService.syncPendingSessions()

        assertEquals(SessionSyncResult.AUTH_ERROR, result)
        // Sessions remain PENDING
        val pending = repository.getPendingSessions(10)
        assertEquals(1, pending.size)
        assertEquals(SyncStatus.PENDING, pending[0].syncStatus)
    }

    @Test
    fun sync_serverError_marksFailedAndReturnsServerError() = runBlocking {
        val sessions = listOf(
            createSession("com.app.one", 1000L, 2000L)
        )
        repository.saveSessions(sessions, 2000L)
        fakeApi.shouldFailWithCode = 500

        val result = syncService.syncPendingSessions()

        assertEquals(SessionSyncResult.SERVER_ERROR, result)
        
        // Session should be marked FAILED with error message
        val dbSessions = database.sessionDao().getSessionsBetween(0L, 10000L)
        assertEquals(1, dbSessions.size)
        assertEquals("FAILED", dbSessions[0].syncStatus)
        assertEquals("Server error: 500", dbSessions[0].errorMessage)
    }

    @Test
    fun sync_networkError_keepsPendingAndReturnsNetworkError() = runBlocking {
        val sessions = listOf(
            createSession("com.app.one", 1000L, 2000L)
        )
        repository.saveSessions(sessions, 2000L)
        fakeApi.shouldThrowNetworkError = true

        val result = syncService.syncPendingSessions()

        assertEquals(SessionSyncResult.NETWORK_ERROR, result)
        
        // Session should remain PENDING
        val dbSessions = database.sessionDao().getSessionsBetween(0L, 10000L)
        assertEquals(1, dbSessions.size)
        assertEquals("PENDING", dbSessions[0].syncStatus)
    }

    @Test
    fun sync_partialSuccess_returnsPartialSuccess() = runBlocking {
        fakeApi.failOnCallIndex = 1
        fakeApi.shouldFailWithCode = 500

        val sessions = mutableListOf<Session>()
        for (i in 1..75) {
            sessions.add(createSession("com.app.$i", i * 1000L, i * 1000L + 500L))
        }
        repository.saveSessions(sessions, 100000L)

        val result = syncService.syncPendingSessions()

        assertEquals(SessionSyncResult.PARTIAL_SUCCESS, result)

        val dbSessions = database.sessionDao().getSessionsBetween(0L, 200000L)
        val sentCount = dbSessions.count { it.syncStatus == "SENT" }
        val failedCount = dbSessions.count { it.syncStatus == "FAILED" }
        assertEquals(50, sentCount)
        assertEquals(25, failedCount)
    }

    private fun createSession(packageName: String, startTime: Long, endTime: Long): Session {
        val startInstant = Instant.ofEpochMilli(startTime)
        val endInstant = Instant.ofEpochMilli(endTime)
        return Session(
            id = SessionIdGenerator.generate(packageName, startInstant, endInstant),
            packageName = packageName,
            appName = packageName,
            startTimeUtc = startInstant,
            endTimeUtc = endInstant,
            durationSeconds = (endTime - startTime) / 1000L,
            syncStatus = SyncStatus.PENDING,
            createdAtUtc = Instant.now()
        )
    }

    class FakeSessionApi : SessionApi {
        var shouldFailWithCode: Int? = null
        var shouldThrowNetworkError = false
        val uploadedBatches = mutableListOf<List<SessionUploadDto>>()
        var lastAuthHeader: String? = null
        var failOnCallIndex: Int? = null
        private var callCount = 0

        override suspend fun uploadSessions(
            authToken: String,
            sessions: List<SessionUploadDto>
        ): Response<Unit> {
            lastAuthHeader = authToken
            val currentCall = callCount
            callCount++

            if (shouldThrowNetworkError && (failOnCallIndex == null || currentCall == failOnCallIndex)) {
                throw IOException("Network simulation failure")
            }

            val code = shouldFailWithCode
            if (code != null && (failOnCallIndex == null || currentCall == failOnCallIndex)) {
                return Response.error(code, okhttp3.ResponseBody.create(null, ""))
            }

            uploadedBatches.add(sessions)
            return Response.success(Unit)
        }
    }
}
