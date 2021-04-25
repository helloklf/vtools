package com.omarea.library.basic

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import java.util.*

class LauncherApps(private val context: Context) {
    // MIUI的设置也算个桌面，什么鬼
    // 启动器应用（桌面）
    val launcherApps: ArrayList<String>
        get() {
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_HOME)
            val resolveinfoList: List<ResolveInfo> = context.packageManager.queryIntentActivities(resolveIntent, 0)
            val launcherApps = ArrayList<String>()
            for (resolveInfo in resolveinfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                if ("com.android.settings" != packageName) { // MIUI的设置有算个桌面，什么鬼
                    launcherApps.add(packageName)
                }
            }
            return launcherApps
        }
}