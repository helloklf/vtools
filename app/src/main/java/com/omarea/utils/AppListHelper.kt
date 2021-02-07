package com.omarea.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.omarea.model.Appinfo
import java.io.File
import java.util.*


/**
 * Created by helloklf on 2017/12/01.
 */
class AppListHelper(context: Context) {
    var packageManager: PackageManager

    private fun exclude(packageName: String): Boolean {
        if (packageName.endsWith(".Pure")) {
            return true
        }
        return false
    }

    fun getTags(applicationInfo: ApplicationInfo): String {
        val stateTags = StringBuilder()
        val readDir = CommonCmds.AbsBackUpDir
        try {
            if (!applicationInfo.enabled) {
                stateTags.append("❄已冻结 ")
            }
            if ((applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED) != 0) {
                stateTags.append("🚫已停用 ")
            }
            if (isSystemApp(applicationInfo) && applicationInfo.sourceDir.startsWith("/data")) {
                stateTags.append("🔒更新的系统应用 ")
            }
            val packageName = applicationInfo.packageName
            val absPath = readDir + packageName + ".apk"
            if (File(absPath).exists()) {
                val backupInfo = packageManager.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES)!!
                val installInfo = packageManager.getPackageInfo(applicationInfo.packageName, 0)
                if (installInfo == null)
                    return ""
                if (backupInfo.versionCode == installInfo.versionCode) {
                    stateTags.append("⭐已备份 ")
                } else if (backupInfo.versionCode > installInfo.versionCode) {
                    stateTags.append("💔低于备份版本 ")
                } else {
                    stateTags.append("♻高于备份版本 ")
                }
            } else if (File(readDir + packageName + ".tar.gz").exists()) {
                stateTags.append("🔄有备份数据 ")
            }
        } catch (ex: Exception) {
        }
        return stateTags.toString().trim()
    }

    /**
     * 检查已安装版本
     */
    fun checkInstall(backupInfo: PackageInfo): String {
        try {
            val installInfo = packageManager.getPackageInfo(backupInfo.packageName, 0)
            if (installInfo == null)
                return ""
            if (backupInfo.versionCode == installInfo.versionCode) {
                return "⭐已安装 "
            } else if (backupInfo.versionCode > installInfo.versionCode) {
                return "💔已安装旧版 "
            } else {
                return "♻已安装新版 "
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return ""
        }
    }

    fun isSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    fun getAppList(systemApp: Boolean? = null, removeIgnore: Boolean = true): ArrayList<Appinfo> {
        val packageInfos = packageManager.getInstalledApplications(0)

        val list = ArrayList<Appinfo>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            val applicationInfo = packageInfos[i]

            val appInfo = getApplicationInfo(applicationInfo, systemApp, removeIgnore)
            if (appInfo != null) {
                list.add(appInfo)
            }
        }
        return (list)
    }

    private fun getApplicationInfo(applicationInfo: ApplicationInfo, systemApp: Boolean? = null, removeIgnore: Boolean = true): Appinfo? {
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

        val item = Appinfo.getItem()
        //val d = packageInfo.loadIcon(packageManager)
        item.appName = applicationInfo.loadLabel(packageManager)
        item.packageName = applicationInfo.packageName
        //item.icon = d
        item.dir = file.parent
        item.enabled = applicationInfo.enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            item.suspended = (applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED) != 0
        }
        item.enabledState = getTags(applicationInfo)
        item.path = appPath
        item.updated = isSystemApp(applicationInfo) && (appPath.startsWith("/data") || (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0)
        item.appType = (if ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
            Appinfo.AppType.SYSTEM
        } else if ((appPath.startsWith("/data") || (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0)) {
            Appinfo.AppType.USER
        } else {
            Appinfo.AppType.SYSTEM
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

    fun getUserAppList(): ArrayList<Appinfo> {
        return getAppList(false)
    }

    fun getSystemAppList(): ArrayList<Appinfo> {
        return getAppList(true)
    }

    fun getAll(): ArrayList<Appinfo> {
        return getAppList(null, false)
    }

    fun getBootableApps(systemApp: Boolean? = null, removeIgnore: Boolean = true): ArrayList<Appinfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val packageInfos = packageManager.queryIntentActivities(mainIntent, 0)

        val list = ArrayList<Appinfo>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            val applicationInfo = packageInfos[i].activityInfo.applicationInfo
            if (removeIgnore && exclude(applicationInfo.packageName)) {
                continue
            } else if (list.find { it.packageName == applicationInfo.packageName } != null) {
                continue
            }

            // if ((systemApp == false && applicationInfo.sourceDir.startsWith("/system")) || (systemApp == true && !applicationInfo.sourceDir.startsWith("/system")))
            //    continue
            if ((systemApp == false && isSystemApp(applicationInfo)) || (systemApp == true && !isSystemApp(applicationInfo)))
                continue

            val file = File(applicationInfo.publicSourceDir)
            if (!file.exists())
                continue

            val item = Appinfo.getItem()
            //val d = packageInfo.loadIcon(packageManager)
            item.appName = applicationInfo.loadLabel(packageManager)
            item.packageName = applicationInfo.packageName
            //item.icon = d
            item.dir = file.parent
            item.enabled = applicationInfo.enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                item.suspended = (applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED) != 0
            }
            item.enabledState = getTags(applicationInfo)
            item.path = applicationInfo.sourceDir
            item.updated = isSystemApp(applicationInfo) && file.parent.startsWith("/data")
            // item.appType = if (applicationInfo.sourceDir.startsWith("/system")) Appinfo.AppType.SYSTEM else Appinfo.AppType.USER
            item.appType = if ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) Appinfo.AppType.USER else Appinfo.AppType.SYSTEM
            try {
                val packageInfo = packageManager.getPackageInfo(applicationInfo.packageName, 0)
                item.versionName = packageInfo.versionName
                item.versionCode = packageInfo.versionCode
            } catch (ex: Exception) {
            }

            list.add(item)
        }
        return (list)
    }

    fun getBackupedAppList(): ArrayList<Appinfo> {
        val dirPath = CommonCmds.AbsBackUpDir
        val list = ArrayList<Appinfo>()
        val dir = File(dirPath)
        if (!dir.exists())
            return list

        if (!dir.isDirectory) {
            dir.delete()
            dir.mkdirs()
            return list
        }
        if (!dir.canRead()) {
            return list
        }

        val files = dir.listFiles { name ->
            name.extension.toLowerCase() == "apk"
        }

        if (files == null) {
            return list
        }

        for (i in files.indices) {
            val absPath = files[i].absolutePath
            try {
                val packageInfo = packageManager.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES)
                if (packageInfo != null) {
                    val applicationInfo = packageInfo.applicationInfo
                    applicationInfo.sourceDir = absPath
                    applicationInfo.publicSourceDir = absPath

                    val item = Appinfo.getItem()
                    item.selectState = false
                    item.appName = applicationInfo.loadLabel(packageManager).toString() + "  (" + packageInfo.versionCode + ")"
                    item.packageName = applicationInfo.packageName
                    item.path = applicationInfo.sourceDir
                    item.enabledState = checkInstall(packageInfo)
                    item.versionName = packageInfo.versionName
                    item.versionCode = packageInfo.versionCode
                    item.appType = Appinfo.AppType.BACKUPFILE
                    list.add(item)
                }
            } catch (ex: Exception) {
            }
        }

        return list
    }

    fun getApp(packageName: String): Appinfo? {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            return getApplicationInfo(applicationInfo, null, false)
        } catch (ex: java.lang.Exception) {
        }
        return null
    }

    init {
        packageManager = context.packageManager
    }
}
