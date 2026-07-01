package com.example

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val installationId = remember(context) {
        InstallationIdStore(context).getOrCreateInstallationId()
    }
    var hasPermission by remember { mutableStateOf(UsagePermissionHelper.hasUsageAccessPermission(context)) }
    var recentPackages by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            recentPackages = UsageStatsReader.getRecentAppPackages(context)
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
