package com.example

import android.util.Log

class DashboardService(
    private val api: DashboardApi
) {
    suspend fun getSummary(
        token: String,
        from: String? = null,
        to: String? = null
    ): DashboardSummaryResponse {
        Log.d("DashboardDebug", "Token: $token")
        return api.getSummary("Bearer $token", from, to)
    }

    suspend fun getTimeline(token: String) =
        api.getTimeline("Bearer $token")

    suspend fun getDeviceUsage(
        token: String,
        from: String? = null,
        to: String? = null
    ): DeviceUsageSummaryResponse {
        return api.getDeviceUsage("Bearer $token", from, to)
    }
}
