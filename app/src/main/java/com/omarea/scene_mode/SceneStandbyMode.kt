package com.omarea.scene_mode

import android.content.Context
import com.omarea.common.shell.KeepShell
import com.omarea.model.Appinfo.AppType.SYSTEM
import com.omarea.model.Appinfo.AppType.USER
import com.omarea.utils.AppListHelper
import com.omarea.vtools.R

class SceneStandbyMode(private val context: Context, private val keepShell: KeepShell) {
    public fun getCmds(on: Boolean): String {
        val apps = AppListHelper(context).getAll()
        val command = if (on) "suspend" else "unsuspend"

        val blackListConfig = context.getSharedPreferences("SceneStandbyList", Context.MODE_PRIVATE)
        val whiteList = context.resources.getStringArray(R.array.scene_standby_white_list)
        val cmds = StringBuffer()
        for (app in apps) {
            if (!whiteList.contains(app.packageName)) {
                if (
                        (app.appType == SYSTEM && blackListConfig.getBoolean(app.packageName.toString(), false)) ||
                        (app.appType == USER && blackListConfig.getBoolean(app.packageName.toString(), true))
                ) {
                    cmds.append("pm ")
                    cmds.append(command)
                    cmds.append(" \"")
                    cmds.append(app.packageName)
                    cmds.append("\"\n")
                    if (on) {
                        cmds.append("am force-stop ")
                        cmds.append(" \"")
                        cmds.append(app.packageName)
                        cmds.append("\"\n")
                    }
                }
            }
        }
        if (on) {
            cmds.append("\n")
            cmds.append("sync\n")
            cmds.append("echo 3 > /proc/sys/vm/drop_caches\n")
            cmds.append("input keyevent 3\n") // 主页键
            // TODO:判断当场是否已经在锁屏状态再执行
            cmds.append("input keyevent 26\n") // 锁屏键
            cmds.append("dumpsys deviceidle step\n")
            cmds.append("dumpsys deviceidle step\n")
            cmds.append("dumpsys deviceidle step\n")
            cmds.append("dumpsys deviceidle step\n")
            cmds.append("\n")
        } else {

        }
        return cmds.toString()
    }

    public fun on() {
        keepShell.doCmdSync(getCmds(true))
    }

    public fun off() {
        keepShell.doCmdSync(getCmds(false))
    }
}
