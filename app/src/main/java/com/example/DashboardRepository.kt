package com.example

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DashboardRepository(
    private val context: android.content.Context
) {
    private val dashboardService: DashboardService by lazy {
        val dashboardApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DashboardApi::class.java)
        DashboardService(dashboardApi)
    }

    suspend fun getDashboardSummary(): Result<DashboardSummaryResponse> {
        return try {
            val tokenStore = TokenStore(context)
            val token = tokenStore.getToken()
            
            if (token == null) {
                return Result.failure(Exception("Token not found"))
            }

            Log.d("DashboardDebug", "Token prefix: ${token.take(20)}")
            
            // Generate today's date range
            val formatter = DateTimeFormatter.ISO_DATE_TIME
            val now = LocalDateTime.now()
            val from = now.withHour(0).withMinute(0).withSecond(0).withNano(0).format(formatter)
            val to = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999).format(formatter)

            Log.d("DashboardDebug", "From: $from")
            Log.d("DashboardDebug", "To: $to")
            val summary = dashboardService.getSummary(token, from, to)
            Log.d("DashboardDebug", "Response: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e("DashboardDebug", "API Error", e)
            Result.failure(e)
        }
    }
}
