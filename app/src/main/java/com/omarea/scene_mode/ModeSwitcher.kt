package com.omarea.scene_mode

import android.content.Context
import com.omarea.Scene
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.library.shell.PropsUtils
import com.omarea.store.CpuConfigStorage
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

/**
 * Created by Hello on 2018/06/03.
 */

open class ModeSwitcher {
    private var inited = false

    companion object {
        const val SOURCE_UNKNOWN = "UNKNOWN"
        const val SOURCE_SCENE_ACTIVE = "SOURCE_SCENE_ACTIVE"
        const val SOURCE_SCENE_CONSERVATIVE = "SOURCE_SCENE_CONSERVATIVE"
        const val SOURCE_SCENE_CUSTOM = "SOURCE_SCENE_CUSTOM"
        const val SOURCE_SCENE_IMPORT = "SOURCE_SCENE_IMPORT"
        const val SOURCE_SCENE_ONLINE = "SOURCE_SCENE_ONLINE"
        const val SOURCE_OUTSIDE = "SOURCE_OUTSIDE"
        const val SOURCE_NONE = "SOURCE_NONE"

        fun getCurrentSource (): String {
            if (CpuConfigInstaller().outsideConfigInstalled()) {
                return SOURCE_OUTSIDE
            }
            val config = Scene.context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getString(SpfConfig.GLOBAL_SPF_PROFILE_SOURCE, SOURCE_UNKNOWN)
            if (config == SOURCE_SCENE_CUSTOM || CpuConfigInstaller().insideConfigInstalled()) {
                return config!!
            }
            return SOURCE_NONE
        }
        fun getCurrentSourceName (): String {
            val source = getCurrentSource()
            return (when (source) {
                "SOURCE_OUTSIDE" -> {
                    "外部来源"
                }
                "SOURCE_SCENE_CONSERVATIVE" -> {
                    "Scene-经典"
                }
                "SOURCE_SCENE_ACTIVE" -> {
                    "Scene-性能"
                }
                "SOURCE_SCENE_CUSTOM" -> {
                    "自定义"
                }
                "SOURCE_SCENE_IMPORT" -> {
                    "文件导入"
                }
                "SOURCE_SCENE_ONLINE" -> {
                    "在线下载"
                }
                "SOURCE_NONE" -> {
                    "未定义"
                }
                else -> {
                    "未知"
                }
            })
        }

        // 是否已经完成内置配置文件的自动更新（如果使用的是Scene自带的配置，每次切换调度前，先安装配置）
        private var innerConfigUpdated = false

        const val OUTSIDE_POWER_CFG_PATH = "/data/powercfg.sh"
        const val OUTSIDE_POWER_CFG_BASE = "/data/powercfg-base.sh"

        internal var DEFAULT = "balance"
        internal var POWERSAVE = "powersave"
        internal var PERFORMANCE = "performance"
        internal var FAST = "fast"
        internal var BALANCE = "balance"
        internal var IGONED = "igoned"
        private var INIT = "init"

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
            POWERSAVE -> R.drawable.shortcut_p1
            BALANCE -> R.drawable.shortcut_p2
            PERFORMANCE -> R.drawable.shortcut_p3
            FAST -> R.drawable.shortcut_p4
            else -> R.drawable.shortcut_p3
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

    private var configProvider: String = ""

    // init
    internal fun initPowercfg(): ModeSwitcher {
        if (configProvider.isEmpty()) {
            val installer = CpuConfigInstaller()
            if (installer.outsideConfigInstalled()) {
                configProvider = OUTSIDE_POWER_CFG_PATH
            } else {
                if (installer.insideConfigInstalled() && !innerConfigUpdated && !anyModeReplaced()) {
                    installer.applyConfigNewVersion(Scene.context)
                    innerConfigUpdated = true
                }
                configProvider = FileWrite.getPrivateFilePath(Scene.context, "powercfg.sh")
            }
        }

        if (configProvider.isNotEmpty()) {
            keepShellExec("sh $configProvider $INIT")
            setCurrentPowercfg("")
        }

        inited = true
        return this
    }

    // 切换模式
    internal fun executePowercfgMode(mode: String): ModeSwitcher {
        if (!inited) {
            initPowercfg()
        }

        val cpuConfigStorage = CpuConfigStorage(Scene.context)
        if (cpuConfigStorage.exists(mode)) {
            cpuConfigStorage.applyCpuConfig(Scene.context, mode)
        } else if (configProvider.isNotEmpty()) {
            keepShellExec("sh $configProvider $mode")
        } else {
            return this
        }
        setCurrentPowercfg(mode)
        return this
    }

    internal fun executePowercfgMode(mode: String, app: String): ModeSwitcher {
        executePowercfgMode(mode)
        setCurrentPowercfgApp(app)
        return this
    }

    // 是否已经完成指定模式的自定义
    public fun modeReplaced(mode: String): Boolean {
        return CpuConfigStorage(Scene.context).exists(mode)
    }

    // 是否已完成四个模式的配置
    public fun modeConfigCompleted(): Boolean {
        if (CpuConfigInstaller().outsideConfigInstalled()) {
            return true
        } else {
            val config = getCurrentSource()
            when (config) {
                SOURCE_SCENE_CUSTOM -> {
                    return allModeReplaced()
                }
                SOURCE_SCENE_ACTIVE,
                SOURCE_SCENE_CONSERVATIVE,
                SOURCE_SCENE_IMPORT,
                SOURCE_SCENE_ONLINE -> {
                    return CpuConfigInstaller().insideConfigInstalled()
                }
            }
        }
        return false
    }

    // 是否已经完成所有模式的自定义
    public fun allModeReplaced(): Boolean {
        val storage = CpuConfigStorage(Scene.context)

        return storage.exists(POWERSAVE) &&
                storage.exists(BALANCE) &&
                storage.exists(PERFORMANCE) &&
                storage.exists(FAST)
    }

    public fun anyModeReplaced(): Boolean {
        val storage = CpuConfigStorage(Scene.context)

        return storage.exists(POWERSAVE) ||
                storage.exists(BALANCE) ||
                storage.exists(PERFORMANCE) ||
                storage.exists(FAST)
    }

    public fun clearInitedState() {
        inited = false
    }
}
