package com.harshit.crossdevicetracker

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DashboardApi {
    @GET("api/dashboard/summary")
    suspend fun getSummary(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): DashboardSummaryResponse

    @GET("api/dashboard/apps")
    suspend fun getAppUsage(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("deviceId") deviceId: String? = null,
        @Query("platform") platform: String? = null
    ): List<AppUsageResponse>

    @GET("api/dashboard/devices")
    suspend fun getDeviceUsage(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): DeviceUsageSummaryResponse

    @GET("api/dashboard/timeline")
    suspend fun getTimeline(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): TimelineResponse

    @GET("api/analytics/daily")
    suspend fun getDailyAnalytics(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): DailyUsageResponse

    @GET("api/analytics/weekly")
    suspend fun getWeeklyAnalytics(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): WeeklyUsageResponse

    @GET("api/analytics/monthly")
    suspend fun getMonthlyAnalytics(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): MonthlyUsageResponse

    @GET("api/analytics/hourly")
    suspend fun getHourlyAnalytics(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): HourlyUsageResponse
}
