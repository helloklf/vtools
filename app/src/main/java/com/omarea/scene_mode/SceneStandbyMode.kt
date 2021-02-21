package com.omarea.scene_mode

import android.content.Context
import com.omarea.common.shell.KeepShell
import com.omarea.model.AppInfo.AppType.SYSTEM
import com.omarea.model.AppInfo.AppType.USER
import com.omarea.utils.AppListHelper
import com.omarea.vtools.R

class SceneStandbyMode(private val context: Context, private val keepShell: KeepShell) {
    companion object {
        public val configSpfName = "SceneStandbyList"
    }

    private val stateProp = "persist.vtools.suspend"

    public fun getCmds(on: Boolean): String {
        val cmds = StringBuffer()
        if (on) {
            val apps = AppListHelper(context).getAll()
            val command = if (on) "suspend" else "unsuspend"

            val blackListConfig = context.getSharedPreferences(configSpfName, Context.MODE_PRIVATE)
            val whiteList = context.resources.getStringArray(R.array.scene_standby_white_list)
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
            cmds.append("\n")
            cmds.append("sync\n")
            cmds.append("echo 3 > /proc/sys/vm/drop_caches\n")
            cmds.append("setprop ")
            cmds.append(stateProp)
            cmds.append(" 1")
            cmds.append("\n")
        } else {
            cmds.append("for app in `pm list package | cut -f2 -d ':'`; do\n" +
                    "      pm unsuspend \$app 1 > /dev/null\n" +
                    "    done\n")
            cmds.append("setprop ")
            cmds.append(stateProp)
            cmds.append(" 0")
            cmds.append("\n")
        }
        return cmds.toString()
    }

    public fun on() {
        if (keepShell.doCmdSync("getprop $stateProp").equals("1")) {
            return
        }
        keepShell.doCmdSync(getCmds(true))
    }

    public fun off() {
        if (keepShell.doCmdSync("getprop $stateProp").equals("0")) {
            return
        }
        keepShell.doCmdSync(getCmds(false))
    }
}
