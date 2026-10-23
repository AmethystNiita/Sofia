package com.amethystniita.sofia

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object AppUtils {
    fun getInstalledAppPackages(context: Context): List<String> {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA
        val installedApps = pm.getInstalledApplications(flags)

        return installedApps.filter { app ->
            val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            (!isSystemApp || isUpdatedSystemApp) && app.packageName != context.packageName
        }.map { it.packageName }
    }
}