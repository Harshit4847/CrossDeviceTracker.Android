package com.harshit.crossdevicetracker

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TimelineRepository(
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

    suspend fun getTimeline(): Result<TimelineResponse> {
        return try {
            val tokenStore = TokenStore(context)
            val token = tokenStore.getToken()
            
            if (token == null) {
                return Result.failure(Exception("Token not found"))
            }
            
            val timeline = dashboardService.getTimeline(token)
            Result.success(timeline)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
