package com.omarea.shared

import android.content.Context
import com.omarea.shell.KeepShellAsync
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.Props
import com.omarea.vtools.R

/**
 * Created by Hello on 2018/06/03.
 */

open class ModeList {
    companion object {
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
    }

    internal var keepShellAsync: KeepShellAsync? = null
    private var context: Context? = null
    private var currentPowercfg: String = ""
    private var currentPowercfgApp: String = ""

    constructor() {
    }

    constructor(context: Context) {
        this.context = context
    }

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

    internal fun getCurrentPowerMode(): String? {
        if (!this.currentPowercfg.isEmpty()) {
            return this.currentPowercfg
        }
        return Props.getProp("vtools.powercfg")
    }

    internal fun getCurrentPowermodeApp(): String? {
        if (!this.currentPowercfgApp.isEmpty()) {
            return this.currentPowercfgApp
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
        /*
        if (keepShellAsync == null) {
            keepShellAsync = KeepShellAsync(context)
        }
        keepShellAsync!!.doCmd(cmd)
        */
        KeepShellPublic.doCmdSync(cmd)
    }

    internal fun executePowercfgMode(mode: String): ModeList {
        keepShellExec("sh ${CommonCmds.POWER_CFG_PATH} " + mode)
        setCurrentPowercfg(mode)
        return this
    }

    internal fun executePowercfgMode(mode: String, app: String): ModeList {
        executePowercfgMode(mode)
        setCurrentPowercfgApp(app)
        return this
    }

    internal fun densityKeepShell(): ModeList {
        if (keepShellAsync != null) {
            keepShellAsync!!.tryExit()
            keepShellAsync = null
        }
        return this
    }


    internal fun executePowercfgModeOnce(mode: String): ModeList {
        KeepShellPublic.doCmdSync("sh ${CommonCmds.POWER_CFG_PATH} " + mode)
        setCurrentPowercfg(mode)
        return this
    }

    internal fun executePowercfgModeOnce(mode: String, app: String): ModeList {
        KeepShellPublic.doCmdSync("sh ${CommonCmds.POWER_CFG_PATH} " + mode)
        setCurrent(mode, app)
        return this
    }
}
