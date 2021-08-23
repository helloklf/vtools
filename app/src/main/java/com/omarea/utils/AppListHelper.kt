package com.omarea.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.omarea.model.AppInfo
import java.io.File
import java.util.*


/**
 * Created by helloklf on 2017/12/01.
 */
class AppListHelper(context: Context) {
    private var packageManager: PackageManager = context.packageManager

    private fun exclude(packageName: String): Boolean {
        if (packageName.endsWith(".Pure")) {
            return true
        }
        return false
    }

    private fun isSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun getAppList(systemApp: Boolean? = null, removeIgnore: Boolean = true): ArrayList<AppInfo> {
        val packageInfos = packageManager.getInstalledApplications(0)

        val list = ArrayList<AppInfo>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            val applicationInfo = packageInfos[i]

            val appInfo = getApplicationInfo(applicationInfo, systemApp, removeIgnore)
            if (appInfo != null) {
                list.add(appInfo)
            }
        }
        return (list)
    }

    private fun getApplicationInfo(applicationInfo: ApplicationInfo, systemApp: Boolean? = null, removeIgnore: Boolean = true): AppInfo? {
        val appPath = applicationInfo.sourceDir
        if (appPath == null || (removeIgnore && exclude(applicationInfo.packageName))) {
            return null
        }

        if (
        // appPath.startsWith("/vendor") ||
                (systemApp == false && !(appPath.startsWith("/data") || (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0)) ||
                (systemApp == true && (appPath.startsWith("/data") || (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0))
        ) {
            return null
        }

        // ApplicationInfo.FLAG_SYSTEM

        val file = File(applicationInfo.publicSourceDir)
        if (!file.exists())
            return null

        val item = AppInfo.getItem()
        //val d = packageInfo.loadIcon(packageManager)
        item.appName = "" + applicationInfo.loadLabel(packageManager)
        item.packageName = applicationInfo.packageName
        //item.icon = d
        item.dir = file.parent
        item.enabled = applicationInfo.enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            item.suspended = (applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED) != 0
        }
        item.path = appPath
        item.updated = isSystemApp(applicationInfo) && (appPath.startsWith("/data") || (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0)
        item.appType = (if ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
            AppInfo.AppType.SYSTEM
        } else if ((appPath.startsWith("/data") || (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0)) {
            AppInfo.AppType.USER
        } else {
            AppInfo.AppType.SYSTEM
        })
        item.targetSdkVersion = applicationInfo.targetSdkVersion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            item.minSdkVersion = applicationInfo.minSdkVersion
        }

        try {
            val packageInfo = packageManager.getPackageInfo(applicationInfo.packageName, 0)
            item.versionName = packageInfo.versionName
            item.versionCode = packageInfo.versionCode
        } catch (ex: Exception) {
        }

        return item
    }

    fun getAll(): ArrayList<AppInfo> {
        return getAppList(null, false)
    }
}
