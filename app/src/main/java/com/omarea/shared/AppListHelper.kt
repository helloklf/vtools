package com.omarea.shared

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.omarea.shared.model.Appinfo
import java.io.File
import java.util.*
import android.content.Intent



/**
 * Created by helloklf on 2017/12/01.
 */
class AppListHelper(context: Context) {
    var packageManager: PackageManager

    //应用忽略列表 一些关键性的应用
    internal var ignore: ArrayList<String> = object : ArrayList<String>() {
        init {
            add(context.packageName)

            //add("com.android.mms")
            //add("com.android.providers.media")
            //add("com.android.packageinstaller")
            //add("com.miui.packageinstaller")
            //add("com.google.android.packageinstaller")
            //add("com.android.defcountainer")
            //add("com.android.settings")
            //add("com.android.providers.settings")
            //add("com.android.vpndialogs")
            //add("com.android.shell")
            //add("com.android.phone")
            //add("com.android.onetimeinitializer")
            //add("com.android.providers.contacts")
            //add("com.android.providers.blockednumber")
            //add("com.android.contacts")
            //add("com.android.providers.telephony")
            //add("com.android.incallui")
            //add("com.android.systemui")
            //add("com.android.providers.downloads.ui")
            //add("com.android.providers.downloads")
            //add("android")
            //add("com.android.carrierconfig")
            //add("com.android.frameworks.telresources")
            //add("com.android.keyguard")
            //add("com.android.wallpapercropper")
            //add("com.miui.rom")
            //add("com.miui.system")
            //add("com.qualcomm.location")
            //add("com.google.android.webview")
            //add("com.android.webview")
        }
    }

    private fun exclude(packageName: String): Boolean {
        if (packageName.endsWith(".Pure")) {
            return true
        }
        return false
    }

    /**
     * 验证已备份版本
     */
    fun checkBackup(packageInfo: ApplicationInfo): String {
        try {
            val packageName = packageInfo.packageName
            val absPath = CommonCmds.AbsBackUpDir + packageName + ".apk"
            if (File(absPath).exists()) {
                val backupInfo = packageManager.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES)
                val installInfo = packageManager.getPackageInfo(packageInfo.packageName, 0)
                if (installInfo == null)
                    return ""
                if (backupInfo.versionCode == installInfo.versionCode) {
                    return "✔"
                } else if (backupInfo.versionCode > installInfo.versionCode) {
                    return "✘"
                } else {
                    return "★"
                }
            } else if (File(CommonCmds.BackUpDir + packageName + ".tar.gz").exists()) {
                return "☆"
            } else {
                return ""
            }
        } catch (ex: Exception) {
            return ""
        }
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
                return "✔"
            } else if (backupInfo.versionCode > installInfo.versionCode) {
                return "✘"
            } else {
                return "★"
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return ""
        }
    }

    fun isSystemApp (applicationInfo: ApplicationInfo): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) !=0 || (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    fun getAppList(systemApp: Boolean? = null, removeIgnore: Boolean = true): ArrayList<Appinfo> {
        val packageInfos = packageManager.getInstalledApplications(0)

        val list = ArrayList<Appinfo>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            val applicationInfo = packageInfos[i]
            if (removeIgnore && ignore.contains(applicationInfo.packageName) || exclude(applicationInfo.packageName)) {
                continue
            }

            // if ((systemApp == false && applicationInfo.sourceDir.startsWith("/system")) || (systemApp == true && !applicationInfo.sourceDir.startsWith("/system")))
            //    continue
            if ((systemApp == false && isSystemApp(applicationInfo)) || (systemApp == true && !isSystemApp(applicationInfo)))
                continue
            // ApplicationInfo.FLAG_SYSTEM

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
            item.enabledState = checkBackup(applicationInfo)
            item.wranState = if (applicationInfo.enabled) "" else "已冻结"
            item.path = applicationInfo.sourceDir
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

    fun getUserAppList(): ArrayList<Appinfo> {
        return getAppList(false)
    }

    fun getSystemAppList(): ArrayList<Appinfo> {
        return getAppList(true)
    }

    fun getAll(): ArrayList<Appinfo> {
        return getAppList(null, false)
    }

    fun getBootableApps(): ArrayList<Appinfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val packageInfos = packageManager.queryIntentActivities(mainIntent, 0)

        val list = ArrayList<Appinfo>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            val applicationInfo = packageInfos[i].activityInfo.applicationInfo

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
            item.enabledState = checkBackup(applicationInfo)
            item.wranState = if (applicationInfo.enabled) "" else "已冻结"
            item.path = applicationInfo.sourceDir
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

    fun getApkFilesInfoList(dirPath: String): ArrayList<Appinfo> {
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

        val files = dir.listFiles({ name ->
            name.extension.toLowerCase() == "apk"
        })

        if (files == null) {
            return list
        }

        for (i in files.indices) {
            val absPath = files[i].absolutePath
            try {
                val packageInfo = packageManager.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES)
                if (packageInfo != null) {
                    val applicationInfo = packageInfo.applicationInfo
                    if (applicationInfo.packageName == "com.omarea.vtools")
                        continue
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

    init {
        packageManager = context.packageManager
    }
}
