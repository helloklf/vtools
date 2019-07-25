package com.omarea.dynamic

import com.omarea.common.shell.KeepShellAsync
import com.omarea.common.shell.KeepShellPublic
import com.omarea.shell.Props
import com.omarea.vtools.R

/**
 * Created by Hello on 2018/06/03.
 */

open class ModeList {
    companion object {
        const val POWER_CFG_PATH = "/data/powercfg.sh"
        const val POWER_CFG_BASE = "/data/powercfg-base.sh"

        internal var DEFAULT = "balance";
        internal var POWERSAVE = "powersave";
        internal var PERFORMANCE = "performance";
        internal var FAST = "fast";
        internal var BALANCE = "balance";
        internal var IGONED = "igoned";

        internal fun getModName(mode: String): String {
            when (mode) {
                POWERSAVE -> return "省电模式"
                PERFORMANCE -> return "性能模式"
                FAST -> return "极速模式"
                BALANCE -> return "均衡模式"
                IGONED -> return "忽略切换"
                "" -> return "跟随默认"
                else -> return "未知模式"
            }
        }

        private var currentPowercfg: String = ""
        private var currentPowercfgApp: String = ""
    }

    internal var keepShellAsync: KeepShellAsync? = null

    internal fun getModIcon(mode: String): Int {
        when (mode) {
            POWERSAVE -> return R.drawable.p1
            BALANCE -> return R.drawable.p2
            PERFORMANCE -> return R.drawable.p3
            FAST -> return R.drawable.p4
            else -> return R.drawable.p3
        }
    }

    internal fun getModImage(mode: String): Int {
        when (mode) {
            POWERSAVE -> return R.drawable.shortcut_p0
            BALANCE -> return R.drawable.shortcut_p1
            PERFORMANCE -> return R.drawable.shortcut_p2
            FAST -> return R.drawable.shortcut_p3
            else -> return R.drawable.shortcut_p2
        }
    }

    internal fun getCurrentPowerMode(): String {
        if (!currentPowercfg.isEmpty()) {
            return currentPowercfg
        }
        return Props.getProp("vtools.powercfg")
    }

    internal fun getCurrentPowermodeApp(): String {
        if (!currentPowercfgApp.isEmpty()) {
            return currentPowercfgApp
        }
        return Props.getProp("vtools.powercfg_app")
    }

    internal fun setCurrent(powerCfg: String, app: String): ModeList {
        setCurrentPowercfg(powerCfg)
        setCurrentPowercfgApp(app)
        return this
    }

    internal fun setCurrentPowercfg(powerCfg: String): ModeList {
        currentPowercfg = powerCfg
        Props.setPorp("vtools.powercfg", powerCfg)
        return this
    }

    internal fun setCurrentPowercfgApp(app: String): ModeList {
        currentPowercfgApp = app
        Props.setPorp("vtools.powercfg_app", app)
        return this
    }

    internal fun keepShellExec(cmd: String) {
        KeepShellPublic.doCmdSync(cmd)
    }

    internal fun executePowercfgMode(mode: String): ModeList {
        keepShellExec("sh $POWER_CFG_PATH " + mode)
        setCurrentPowercfg(mode)
        return this
    }

    internal fun executePowercfgMode(mode: String, app: String): ModeList {
        executePowercfgMode(mode)
        setCurrentPowercfgApp(app)
        return this
    }

    internal fun destroyKeepShell(): ModeList {
        if (keepShellAsync != null) {
            keepShellAsync!!.tryExit()
            keepShellAsync = null
        }
        return this
    }
}
