package com.example

data class PeriodStats(
    val totalScreenTimeSeconds: Long,
    val totalDeviceUsageSeconds: Long? = null,
    val overlapTimeSeconds: Long? = null,
    val sessionCount: Int
)

data class AppUsageResponse(
    val appName: String,
    val durationSeconds: Long,
    val percentage: Double,
    val sessionCount: Int
)

data class DeviceUsageResponse(
    val deviceName: String,
    val platform: String,
    val durationSeconds: Long,
    val percentage: Double,
    val sessionCount: Int
)

data class TimelineResponse(
    val entries: List<TimelineEntry>
)

data class TimelineEntry(
    val start: String,
    val end: String,
    val app: String,
    val device: String,
    val platform: String,
    val durationSeconds: Long
)

data class DailyUsageResponse(
    val days: List<DayStats>
)

data class DayStats(
    val date: String,
    val durationSeconds: Long,
    val sessionCount: Int
)

data class WeeklyUsageResponse(
    val weeks: List<WeekStats>
)

data class WeekStats(
    val weekStart: String,
    val weekEnd: String,
    val durationSeconds: Long,
    val sessionCount: Int
)

data class MonthlyUsageResponse(
    val months: List<MonthStats>
)

data class MonthStats(
    val year: Int,
    val month: Int,
    val durationSeconds: Long,
    val sessionCount: Int
)

data class HourlyUsageResponse(
    val hours: List<HourStats>
)

data class HourStats(
    val hour: Int,
    val durationSeconds: Long,
    val sessionCount: Int
)
