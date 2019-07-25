package com.omarea.shell_utils

import android.util.Log
import com.omarea.common.shell.KeepShellPublic

/**
 * Created by SYSTEM on 2018/07/19.
 */

class DumpsysUtils {
    /**
     * 获取前台应用
     */
    fun fromDumpsysActivity(packageName: String): String {
        val topActivityResult = KeepShellPublic.doCmdSync("dumpsys activity top | grep ACTIVITY")
        if (topActivityResult == "error") {
            Log.e("dumpsysTopActivity", "精准切换 - 获取前台应用失败！")
        } else {
            val topActivitys = topActivityResult.split("\n");
            var lastActivity = ""
            for (item in topActivitys) {
                if (!item.isEmpty())
                    lastActivity = item.trim()
            }
            if (lastActivity.contains(packageName)) {
                return (packageName)
            } else {
                if (lastActivity.indexOf("/") > 8 && lastActivity.startsWith("ACTIVITY")) {
                    val dumpPackageName = lastActivity.substring(8, lastActivity.indexOf("/")).trim()
                    return (dumpPackageName)
                }
            }
        }
        return ""
    }

    /**
     * 获取当前窗口
     */
    fun fromDumpsysWindow(packageName: String): String {
        try {
            val currentWindow = KeepShellPublic.doCmdSync("dumpsys window w| grep Current")
            if (currentWindow == "error") {
                Log.e("dumpsysTopActivity", "精准切换 - 获取前台应用失败！")
            } else {
                //   mCurrentFocus=Window{6601e94 u0 com.mi.android.globallauncher/com.miui.home.launcher.Launcher}
                if (currentWindow.contains(packageName)) {
                    return packageName
                } else {
                    val columns = currentWindow.substring(currentWindow.indexOf("{") + 1, currentWindow.lastIndexOf("}")).split(" ")
                    if (columns.size == 3 && columns[2].contains("/")) {
                        return columns[2].substring(0, columns[2].indexOf("/"))
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("fromDumpsysWindow", packageName + " - " + ex.message)
        }
        return ""
        //  dumpsys window w| grep Current
    }
}
