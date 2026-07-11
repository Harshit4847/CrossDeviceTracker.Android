package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

object AppInfoHelper {
    
    fun getAppInfo(context: Context, packageName: String): AppInfo? {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(applicationInfo).toString()
            val icon = packageManager.getApplicationIcon(applicationInfo)
            
            AppInfo(
                packageName = packageName,
                label = label,
                icon = icon
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }
    
    fun getAppLabel(context: Context, packageName: String): String {
        return getAppInfo(context, packageName)?.label ?: packageName
    }
    
    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return getAppInfo(context, packageName)?.icon
    }
}
