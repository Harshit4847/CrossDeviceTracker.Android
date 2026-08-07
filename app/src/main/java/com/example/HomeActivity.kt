package com.example

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.room.Room
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.SessionMapper
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.wtf("HARSHIT_TEST", "HomeActivity onCreate called")
        setContent {
            MyApplicationTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(UsagePermissionHelper.hasUsageAccessPermission(context)) }
    var recentPackages by remember { mutableStateOf(listOf<String>()) }
    var pendingSessionsCount by remember { mutableStateOf(0) }
    var lastSyncTime by remember { mutableStateOf<Long?>(null) }
    var todayScreenTime by remember { mutableStateOf(0L) }
    var syncStatus by remember { mutableStateOf<UiSyncStatus>(UiSyncStatus.Idle) }

    val database = remember {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app-database"
        ).build()
    }
    val sessionRepository = remember {
        RoomSessionRepository(
            database.sessionDao(),
            database.trackerMetadataDao(),
            SessionMapper
        )
    }
    val deviceTokenStore = remember { DeviceTokenStore(context) }
    val sessionApi = remember {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SessionApi::class.java)
    }
    val sessionSyncService = remember {
        SessionSyncService(
            sessionRepository,
            sessionApi,
            deviceTokenStore
        )
    }
    val sessionCaptureService = remember {
        SessionCaptureService(
            UsageStatsReader,
            SessionReconstructor(),
            sessionRepository
        )
    }
    val dashboardRepository = remember { DashboardRepository(context) }
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(dashboardRepository)
    )
    val dashboardUiState by dashboardViewModel.uiState.collectAsState()

    // Observe lifecycle events to check permission when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = UsagePermissionHelper.hasUsageAccessPermission(context)
                Log.d("HomeActivity", "onResume: Permission check - $hasPermission")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load dashboard data when permission is granted
    LaunchedEffect(hasPermission) {
        Log.e("HARSHIT_TEST", "hasPermission = $hasPermission")

        if (hasPermission) {
            recentPackages = UsageStatsReader.getRecentAppPackages(context)
            Log.d("HomeActivity", "Calling capture()")
            Log.e("HARSHIT_TEST", "Before capture()")
            sessionCaptureService.capture(context)
            Log.e("HARSHIT_TEST", "After capture()")
            
            // Load dashboard stats
            loadDashboardStats(sessionRepository) { pending, lastSync, screenTime ->
                pendingSessionsCount = pending
                lastSyncTime = lastSync
                todayScreenTime = screenTime
            }

            // Schedule periodic sync (every 15 minutes)
            SyncWorkManager.schedulePeriodicSync(context)

            // Trigger immediate sync on app launch
            SyncWorkManager.triggerImmediateSync(context)
            
            // Observe network connectivity for sync on reconnect
            NetworkConnectivityManager.observeNetworkConnectivity(context)
                .collect { isConnected ->
                    if (isConnected) {
                        Log.d("HomeActivity", "Network reconnected, triggering sync")
                        SyncWorkManager.triggerImmediateSync(context)
                    }
                }
        }
    }
    
    // Observe WorkManager sync status to update UI
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData("one_time_sync_work")
                .observe(lifecycleOwner) { workInfos ->
                    val workInfo = workInfos?.firstOrNull()
                    when (workInfo?.state) {
                        WorkInfo.State.RUNNING -> {
                            syncStatus = UiSyncStatus.Uploading
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            syncStatus = UiSyncStatus.Success
                            // Reload dashboard stats after successful sync
                            CoroutineScope(Dispatchers.IO).launch {
                                loadDashboardStats(sessionRepository) { pending, lastSync, screenTime ->
                                    pendingSessionsCount = pending
                                    lastSyncTime = lastSync
                                    todayScreenTime = screenTime
                                }
                                // Refresh dashboard from backend after sync
                                dashboardViewModel.refresh()
                            }
                            // Reset status after 3 seconds
                            CoroutineScope(Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(3000)
                                syncStatus = UiSyncStatus.Idle
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            syncStatus = UiSyncStatus.Error("Sync Failed")
                            CoroutineScope(Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(3000)
                                syncStatus = UiSyncStatus.Idle
                            }
                        }
                        WorkInfo.State.ENQUEUED -> {
                            if (!NetworkConnectivityManager.isInternetAvailable(context)) {
                                syncStatus = UiSyncStatus.Error("Waiting for network")
                            }
                        }
                        else -> {
                            // IDLE, BLOCKED, CANCELLED
                            syncStatus = UiSyncStatus.Idle
                        }
                    }
                }
        }
    }

        if (hasPermission) {
            DashboardContent(
                context = context,
                dashboardUiState = dashboardUiState,
                pendingSessionsCount = pendingSessionsCount,
                lastSyncTime = lastSyncTime,
                syncStatus = syncStatus,
                recentPackages = recentPackages,
                onRefreshDashboard = { dashboardViewModel.refresh() },
                onOpenTimeline = {
                    val intent = android.content.Intent(context, TimelineActivity::class.java)
                    context.startActivity(intent)
                },
                onOpenDashboard = {
                    val intent = android.content.Intent(context, DashboardActivity::class.java)
                    context.startActivity(intent)
                },
                onSyncNow = {
                    Log.d("SyncButton", "Manual sync pressed")
                    syncStatus = UiSyncStatus.Uploading
                    SyncWorkManager.triggerImmediateSync(context)
                    Toast.makeText(
                        context,
                        "Manual sync started",
                        Toast.LENGTH_SHORT
                    ).show()

                    CoroutineScope(Dispatchers.IO).launch {
                        kotlinx.coroutines.delay(3000)
                        syncStatus = UiSyncStatus.Idle
                    }
                }
            )
        } else {
            PermissionRequiredScreen(context = context)
        }
}

    @Composable
    fun DashboardContent(
        context: android.content.Context,
        dashboardUiState: DashboardUiState,
        pendingSessionsCount: Int,
        lastSyncTime: Long?,
        syncStatus: UiSyncStatus,
        recentPackages: List<String>,
        onRefreshDashboard: () -> Unit,
        onOpenTimeline: () -> Unit,
        onOpenDashboard: () -> Unit,
        onSyncNow: () -> Unit,
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Good Evening",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onRefreshDashboard) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Dashboard"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SyncButton(
                    syncStatus = syncStatus,
                    onSyncClick = onSyncNow
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Screen Time",
                        value = dashboardUiState.screenTime,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Devices",
                        value = dashboardUiState.deviceCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Apps Used",
                        value = dashboardUiState.appCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Sessions",
                        value = dashboardUiState.sessionCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                MostUsedAppCard(
                    appName = dashboardUiState.mostUsedApp,
                    time = dashboardUiState.mostUsedAppTime
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOpenTimeline,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Timeline")
                }

                Spacer(modifier = Modifier.height(16.dp))

                InfoCard(
                    title = "Pending Uploads",
                    value = "$pendingSessionsCount Sessions"
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoCard(
                    title = "Last Sync",
                    value = formatLastSyncTime(lastSyncTime)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOpenDashboard,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("View Dashboard")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Recent Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                RecentAppsList(packages = recentPackages, context = context)
            }
        }
    }

    @Composable
    fun PermissionRequiredScreen(context: android.content.Context) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Permission required",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Grant usage access so we can read app activity and show your dashboard.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                PermissionCard(hasPermission = false, context = context)
            }
        }
    }

@Composable
fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MostUsedAppCard(appName: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Most Used App",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PermissionCard(hasPermission: Boolean, context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasPermission) "🟢" else "🔴",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasPermission) "Permission Granted" else "Permission Not Granted",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            if (!hasPermission) {
                Button(
                    onClick = {
                        UsagePermissionHelper.openUsageAccessSettings(context)
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Grant", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SyncButton(syncStatus: UiSyncStatus, onSyncClick: () -> Unit) {
    Button(
        onClick = onSyncClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = syncStatus != UiSyncStatus.Uploading
    ) {
        when (syncStatus) {
            UiSyncStatus.Uploading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading...")
            }
            UiSyncStatus.Success -> {
                Text("Sync Successful ✓")
            }
            is UiSyncStatus.Error -> {
                Text(syncStatus.message)
            }
            UiSyncStatus.Idle -> {
                Text("Sync Now")
            }
        }
    }
}

@Composable
fun RecentAppsList(packages: List<String>, context: android.content.Context) {
    if (packages.isEmpty()) {
        Text(
            text = "No recent apps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    
    Column {
        packages.forEach { packageName ->
            val appInfo = remember(packageName) {
                AppInfoHelper.getAppInfo(context, packageName)
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                appInfo?.icon?.let { drawable ->
                    Image(
                        painter = BitmapPainter(drawable.toBitmap().asImageBitmap()),
                        contentDescription = appInfo.label,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // App Label
                Text(
                    text = appInfo?.label ?: packageName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (packages.indexOf(packageName) < packages.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                ) {}
            }
        }
    }
}

sealed class UiSyncStatus {
    object Idle : UiSyncStatus()
    object Uploading : UiSyncStatus()
    object Success : UiSyncStatus()
    data class Error(val message: String) : UiSyncStatus()
}

fun formatScreenTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

fun formatLastSyncTime(timestamp: Long?): String {
    if (timestamp == null) return "Never Synced"
    
    val now = System.currentTimeMillis()
    val diffMinutes = (now - timestamp) / (1000 * 60)
    
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
        else -> "${diffMinutes / 1440}d ago"
    }
}

suspend fun loadDashboardStats(
    sessionRepository: SessionRepository,
    onResult: (pendingCount: Int, lastSync: Long?, screenTime: Long) -> Unit
) {
    val pendingSessions = sessionRepository.getPendingSessions(limit = 1000)
    val lastSync = sessionRepository.getLastSuccessfulSync()

    // Calculate today's screen time
    val startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay()
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endOfDay = LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay()
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val todaySessions = sessionRepository.getSessionsBetween(startOfDay, endOfDay)
    val screenTime = todaySessions.sumOf { it.durationSeconds }

    onResult(pendingSessions.size, lastSync, screenTime)
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MyApplicationTheme {
        HomeScreen()
    }
}
