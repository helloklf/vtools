package com.omarea.scene_mode

import android.content.Context
import com.omarea.common.shell.KeepShell
import com.omarea.model.Appinfo.AppType.SYSTEM
import com.omarea.model.Appinfo.AppType.USER
import com.omarea.utils.AppListHelper
import com.omarea.vtools.R

class SceneStandbyMode(private val context: Context, private val keepShell: KeepShell) {
    companion object {
        public val configSpfName = "SceneStandbyList"
    }
    private val stateProp = "persist.vtools.suspend"

    public fun getCmds(on: Boolean): String {
        val apps = AppListHelper(context).getAll()
        val command = if (on) "suspend" else "unsuspend"

        val blackListConfig = context.getSharedPreferences(configSpfName, Context.MODE_PRIVATE)
        val whiteList = context.resources.getStringArray(R.array.scene_standby_white_list)
        val cmds = StringBuffer()
        for (app in apps) {
            if (!whiteList.contains(app.packageName)) {
                if (
                        ((app.appType == SYSTEM || app.updated) && blackListConfig.getBoolean(app.packageName.toString(), false)) ||
                        (app.appType == USER && (!app.updated) && blackListConfig.getBoolean(app.packageName.toString(), true))
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
                        // TODO:真的要这么做吗？
                        // if (app.packageName.equals("com.google.android.gsf")) {
                        //     cmds.append("pm disable com.google.android.gsf 2> /dev/null\n")
                        // }
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
            cmds.append("setprop ")
            cmds.append(stateProp)
            cmds.append(" 1")
            cmds.append("\n")
        } else {
            cmds.append("setprop ")
            cmds.append(stateProp)
            cmds.append(" 0")
            cmds.append("\n")
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
