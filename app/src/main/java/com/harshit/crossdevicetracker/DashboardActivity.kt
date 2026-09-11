package com.harshit.crossdevicetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harshit.crossdevicetracker.ui.theme.MyApplicationTheme

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                DashboardScreen()
            }
        }
    }
}

enum class DashboardTab(val displayName: String) {
    SUMMARY("Summary"),
    APPS("Apps"),
    DEVICES("Devices"),
    TIMELINE("Timeline"),
    ANALYTICS("Analytics")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dashboardRepository = remember { DashboardRepository(context) }
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(dashboardRepository)
    )

    var selectedTab by remember { mutableStateOf(DashboardTab.SUMMARY) }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                DashboardTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val uiState by viewModel.uiState.collectAsState()

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading dashboard",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    // Dashboard content - placeholder for now
                    when(selectedTab){
                        DashboardTab.DEVICES -> {
                            LazyColumn{
                                items(uiState.deviceUsages){device ->
                                    Column(Modifier.padding(16.dp)) {
                                        Text(device.deviceName)
                                        Text(device.platform)
                                        Text(if (device.isActive) "Active" else "Inactive")
                                        Text(formatScreenTime(device.durationSeconds))
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Full Dashboard",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Use Home screen for summary and Timeline for detailed view",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // This activity can be expanded later to show full dashboard
                }
            }
        }
    }
}

@Composable
fun DashboardTabRow(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        DashboardTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.displayName) }
            )
        }
    }
}
