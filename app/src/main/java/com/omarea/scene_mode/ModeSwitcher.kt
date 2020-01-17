package com.omarea.scene_mode

import android.content.Context
import com.omarea.common.shell.KeepShellAsync
import com.omarea.common.shell.KeepShellPublic
import com.omarea.model.CpuStatus
import com.omarea.shell_utils.PropsUtils
import com.omarea.store.CpuConfigStorage
import com.omarea.vtools.R

/**
 * Created by Hello on 2018/06/03.
 */

open class ModeSwitcher {
    companion object {
        const val POWER_CFG_PATH = "/data/powercfg.sh"
        const val POWER_CFG_BASE = "/data/powercfg-base.sh"

        internal var DEFAULT = "balance"
        internal var POWERSAVE = "powersave"
        internal var PERFORMANCE = "performance"
        internal var FAST = "fast"
        internal var BALANCE = "balance"
        internal var IGONED = "igoned"

        internal fun getModName(mode: String): String {
            when (mode) {
                POWERSAVE -> return "省电模式"
                PERFORMANCE -> return "性能模式"
                FAST -> return "极速模式"
                BALANCE -> return "均衡模式"
                IGONED -> return "保持状态"
                "" -> return "全局默认"
                else -> return "未知模式"
            }
        }

        private var currentPowercfg: String = ""
        private var currentPowercfgApp: String = ""
    }

    private var keepShellAsync: KeepShellAsync? = null

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
        return when (mode) {
            POWERSAVE -> R.drawable.shortcut_p0
            BALANCE -> R.drawable.shortcut_p1
            PERFORMANCE -> R.drawable.shortcut_p2
            FAST -> R.drawable.shortcut_p3
            else -> R.drawable.shortcut_p2
        }
    }

    fun getCurrentPowerMode(): String {
        if (!currentPowercfg.isEmpty()) {
            return currentPowercfg
        }
        return PropsUtils.getProp("vtools.powercfg")
    }

    internal fun getCurrentPowermodeApp(): String {
        if (!currentPowercfgApp.isEmpty()) {
            return currentPowercfgApp
        }
        return PropsUtils.getProp("vtools.powercfg_app")
    }

    internal fun setCurrent(powerCfg: String, app: String): ModeSwitcher {
        setCurrentPowercfg(powerCfg)
        setCurrentPowercfgApp(app)
        return this
    }

    internal fun setCurrentPowercfg(powerCfg: String): ModeSwitcher {
        currentPowercfg = powerCfg
        PropsUtils.setPorp("vtools.powercfg", powerCfg)
        return this
    }

    internal fun setCurrentPowercfgApp(app: String): ModeSwitcher {
        currentPowercfgApp = app
        PropsUtils.setPorp("vtools.powercfg_app", app)
        return this
    }

    private fun keepShellExec(cmd: String) {
        KeepShellPublic.doCmdSync(cmd)
    }

    internal fun executePowercfgMode(context: Context, mode: String): ModeSwitcher {
        val cpuConfigStorage = CpuConfigStorage(context)
        val replaced = cpuConfigStorage.load(mode)
        if (replaced != null) {
            cpuConfigStorage.applyCpuConfig(context, replaced)
        } else {
            keepShellExec("sh $POWER_CFG_PATH $mode")
        }
        setCurrentPowercfg(mode)
        return this
    }

    internal fun executePowercfgMode(context: Context, mode: String, app: String): ModeSwitcher {
        executePowercfgMode(context, mode)
        setCurrentPowercfgApp(app)
        return this
    }

    // 是否已经完成指定模式的自定义
    public fun modeReplaced(context: Context, mode: String): CpuStatus? {
        return CpuConfigStorage(context).load(mode)
    }

    // 是否已经完成所有模式的自定义
    public fun allModeReplaced(context: Context): Boolean {
        val storage  = CpuConfigStorage(context)

        return storage.load(POWERSAVE) != null &&
                storage.load(BALANCE) != null &&
                storage.load(PERFORMANCE) != null &&
                storage.load(FAST) != null
    }

    internal fun destroyKeepShell(): ModeSwitcher {
        if (keepShellAsync != null) {
            keepShellAsync!!.tryExit()
            keepShellAsync = null
        }
        return this
    }
}
