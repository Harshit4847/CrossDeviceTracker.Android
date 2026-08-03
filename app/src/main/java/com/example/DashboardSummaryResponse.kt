package com.example

data class DashboardSummaryResponse(
    val today: TodaySummary,
    val deviceCount: Int,
    val appCount: Int,
    val mostUsedApp: MostUsedApp
)

data class TodaySummary(
    val totalScreenTimeSeconds: Long,
    val sessionCount: Int
)

data class MostUsedApp(
    val appName: String,
    val durationSeconds: Long
)
