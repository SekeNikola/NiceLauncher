package com.nicelauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class AppRepository(private val context: Context) {
    private val pm = context.packageManager

    fun getInstalledApps(): List<AppItem> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return activities
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                AppItem(
                    packageName = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String) {
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
