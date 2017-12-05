package com.omarea.units

import android.content.Context
import android.content.pm.PackageManager
import com.omarea.shared.Consts
import java.io.File
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by helloklf on 2017/12/01.
 */

class AppListHelper {
    var packageManager: PackageManager
    constructor(context: Context){
        packageManager = context.packageManager
    }

    internal var ignore: ArrayList<String> = object : ArrayList<String>() {
        init {
            add("com.android.mms")
            add("com.android.providers.media")
            add("com.android.packageinstaller")
            add("com.miui.packageinstaller")
            add("com.google.android.packageinstaller")
            add("com.android.defcountainer")
            add("com.android.settings")
            add("com.android.providers.settings")
            add("com.android.vpndialogs")
            add("com.android.shell")
            add("com.android.phone")
            add("com.android.onetimeinitializer")
            add("com.android.providers.contacts")
            add("com.android.providers.blockednumber")
            add("com.android.contacts")
            add("com.android.providers.telephony")
            add("com.android.incallui")
            add("com.android.systemui")
            add("com.android.providers.downloads.ui")
            add("com.android.providers.downloads")
            add("android")
            add("com.android.carrierconfig")
            add("com.android.frameworks.telresources")
            add("com.android.keyguard")
            add("com.android.wallpapercropper")
            add("com.miui.rom")
            add("com.miui.system")
            add("com.qualcomm.location")
            add("com.google.android.webview")
            add("com.android.webview")
        }
    }

    fun getAppList (systemApp: Boolean? = null, removeIgnore: Boolean = true) : ArrayList<HashMap<String,Any>> {
        val packageInfos = packageManager.getInstalledApplications(0)

        val list = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            val packageInfo = packageInfos[i]

            if (removeIgnore && ignore.contains(packageInfo.packageName)) {
                continue
            }

            if ((systemApp == false && packageInfo.sourceDir.startsWith("/system")) || (systemApp == true && packageInfo.sourceDir.startsWith("/data")))
                continue

            val file = File(packageInfo.publicSourceDir)
            if (!file.exists())
                continue

            val item = HashMap<String, Any>()
            val d = packageInfo.loadIcon(packageManager)
            item.put("icon", d)
            item.put("select_state", false)
            item.put("dir", packageInfo.sourceDir)
            item.put("enabled", packageInfo.enabled)
            item.put("enabled_state", if (File(Consts.BackUpDir + packageInfo.packageName + ".apk").exists()) "已备份" else "")
            item.put("wran_state", if (packageInfo.enabled) "" else "已冻结")

            item.put("name", packageInfo.loadLabel(packageManager))
            item.put("packageName", packageInfo.packageName)
            item.put("path", packageInfo.sourceDir)
            item.put("dir", file.parent)
            list.add(item)
        }
        return (list)
    }

    fun getUserAppList(): ArrayList<HashMap<String, Any>> {
        return getAppList(false)
    }

    fun getSystemAppList(): ArrayList<HashMap<String, Any>> {
        return getAppList(true)
    }

    fun getAll(): ArrayList<HashMap<String, Any>> {
        return getAppList(null, false)
    }

    fun checkInstalled (packageName: String): Boolean {
        try {
            packageManager.getPackageInfo(packageName, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }

    fun getApkFilesInfoList (dirPath: String): ArrayList<HashMap<String,Any>> {
        val list = ArrayList<HashMap<String, Any>>()
        val dir = File(dirPath)
        if (!dir.exists())
            return list

        if (!dir.isDirectory) {
            dir.delete()
            dir.mkdirs()
            return list
        }

        val files = dir.list { dir, name -> name.toLowerCase().endsWith(".apk") }

        for (i in files.indices) {
            val absPath = dirPath + "/" + files[i]
            try {
                val packageInfo = packageManager.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES)
                if (packageInfo != null) {
                    val applicationInfo = packageInfo.applicationInfo
                    applicationInfo.sourceDir = absPath
                    applicationInfo.publicSourceDir = absPath

                    val item = HashMap<String, Any>()
                    val d = applicationInfo.loadIcon(packageManager)
                    item.put("icon", d)
                    item.put("select_state", false)
                    item.put("name", applicationInfo.loadLabel(packageManager).toString() + "  (" + packageInfo.versionCode + ")")
                    item.put("packageName", applicationInfo.packageName)
                    item.put("path", applicationInfo.sourceDir)
                    item.put("enabled_state", if (checkInstalled(applicationInfo.packageName)) "已安装" else "")
                    list.add(item)
                }
            } catch (ex: Exception) {

            }

        }

        return list
    }
}
