package com.example

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

object UsageStatsReader {
    var eventProvider: ((Long?) -> List<UsageEvents.Event>)? = null

    fun readUsageEvents(lastProcessedEventTimestamp: Long?): List<UsageEvents.Event> {
        return eventProvider?.invoke(lastProcessedEventTimestamp) ?: emptyList()
    }

    fun getRecentAppPackages(context: Context, limit: Int = 10): List<String> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val packageNames = linkedSetOf<String>()
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                packageNames.add(event.packageName)
                if (packageNames.size >= limit) {
                    break
                }
            }
        }

        return packageNames.toList()
    }
}
