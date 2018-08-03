package com.omarea.shell

import android.util.Log

/**
 * Created by SYSTEM on 2018/07/19.
 */

class DumpTopAppliction {
    /**
     * 获取前台应用
     */
    fun dumpsysTopActivity(packageName: String): String {
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
}
