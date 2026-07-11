package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.room.Room
import com.example.SessionMapper
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
    val installationId = remember(context) {
        InstallationIdStore(context).getOrCreateInstallationId()
    }
    var hasPermission by remember { mutableStateOf(UsagePermissionHelper.hasUsageAccessPermission(context)) }
    var recentPackages by remember { mutableStateOf(listOf<String>()) }

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

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            recentPackages = UsageStatsReader.getRecentAppPackages(context)
            Log.d("HomeActivity", "Calling capture()")
            sessionCaptureService.capture(context)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(24.dp)) {
            Text(text = "Welcome to Home", style = MaterialTheme.typography.headlineSmall)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(text = "Installation ID")
            Text(text = installationId, style = MaterialTheme.typography.bodyMedium)

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 20.dp))
            Text(text = if (hasPermission) "Permission Granted" else "Permission Not Granted")

            if (!hasPermission) {
                Button(
                    onClick = {
                        UsagePermissionHelper.openUsageAccessSettings(context)
                    },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text("Grant Usage Access")
                }
            } else {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
                Button(
                    onClick = {
                        Log.d("SessionSync", "Sync button clicked")
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = sessionSyncService.syncPendingSessions()
                            Log.d("SessionSync", "Result: $result")
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Sync Now")
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
                Text(text = "Recent apps")
                Text(text = recentPackages.joinToString(""), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MyApplicationTheme {
        HomeScreen()
    }
}
