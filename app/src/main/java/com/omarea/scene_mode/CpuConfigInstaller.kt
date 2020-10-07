package com.omarea.scene_mode

import android.content.Context
import com.omarea.Scene
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.library.shell.PlatformUtils
import com.omarea.store.CpuConfigStorage
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import java.io.File
import java.nio.charset.Charset
import java.util.*

class CpuConfigInstaller {
    val rootDir = "powercfg"

    private fun getPowerCfgDir(): String {
        return rootDir + "/" + PlatformUtils().getCPUName()
    }

    // 移除自定义的各个模式
    fun removeCustomModes(context: Context) {
        val storage = CpuConfigStorage(context)
        storage.remove(ModeSwitcher.POWERSAVE)
        storage.remove(ModeSwitcher.BALANCE)
        storage.remove(ModeSwitcher.PERFORMANCE)
        storage.remove(ModeSwitcher.FAST)
    }

    fun removeOutsideConfig() {
        KeepShellPublic.doCmdSync("rm -f " + ModeSwitcher.OUTSIDE_POWER_CFG_PATH)
        KeepShellPublic.doCmdSync("rm -f " + ModeSwitcher.OUTSIDE_POWER_CFG_BASE)
    }

    // 安装应用内自带的配置
    fun installOfficialConfig(context: Context, afterCmds: String = "", biCore: Boolean = false): Boolean {
        if (!dynamicSupport(context)) {
            return false
        }
        try {
            val powercfg = FileWrite.writePrivateShellFile(getPowerCfgDir() + (if (biCore) "/active.sh" else "/conservative.sh"), "powercfg.sh", context)
            val powercfgBase = FileWrite.writePrivateShellFile(getPowerCfgDir() + (if (biCore) "/active-base.sh" else "/conservative-base.sh"), "powercfg-base.sh", context)

            if (powercfg == null) {
                return false
            } else {
                File(powercfg).run {
                    setExecutable(true, false)
                    setWritable(true)
                    setReadable(true)
                }
                if (powercfgBase != null) {
                    File(powercfgBase).run {
                        setExecutable(true, false)
                        setWritable(true)
                        setReadable(true)
                    }
                }

                ModeSwitcher().setCurrentPowercfg("")
                if (!afterCmds.isEmpty()) {
                    KeepShellPublic.doCmdSync(afterCmds)
                }
                context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).edit().putString(SpfConfig.GLOBAL_SPF_CPU_CONFIG_AUTHOR, "scene").apply()
                removeCustomModes(context)
                return true
            }
        } catch (ex: Exception) {
        }
        return false
    }

    // 尝试更新调度配置文件（目前仅支持自动更新内置的调度文件）
    fun applyConfigNewVersion(context: Context) {
        val config = Scene.context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (config.getString(SpfConfig.GLOBAL_SPF_CPU_CONFIG_AUTHOR, "unknown")?.toLowerCase(Locale.getDefault()) == "scene") {
            installOfficialConfig(context)
        }
    }

    // 安装自定义配置
    fun installCustomConfig(context: Context, powercfg: String, author: String): Boolean {
        try {
            FileWrite.writePrivateFile(powercfg
                    .replace(Regex("\r\n"), "\n").replace(Regex("\r\t"), "\t")
                    .toByteArray(Charset.forName("UTF-8")), "powercfg.sh", context)
            File(FileWrite.getPrivateFilePath(context, "powercfg.sh")).run {
                setExecutable(true, false)
                setWritable(true)
                setReadable(true)
            }
            ModeSwitcher().setCurrentPowercfg("")
            context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).edit().putString(SpfConfig.GLOBAL_SPF_CPU_CONFIG_AUTHOR, author).apply()
            removeCustomModes(context)
            return true
        } catch (ex: Exception) {
            return false
        }
    }

    // 校验编码
    fun configCodeVerify() {
        try {
            val cmd = StringBuilder()
            cmd.append("if [[ -f ${ModeSwitcher.OUTSIDE_POWER_CFG_PATH} ]]; then \n")
            cmd.append("busybox sed -i 's/\\r//' ${ModeSwitcher.OUTSIDE_POWER_CFG_PATH};\n")
            cmd.append("chmod 0775 ${ModeSwitcher.OUTSIDE_POWER_CFG_PATH};\n")
            cmd.append("fi;\n")
            cmd.append("if [[ -f ${ModeSwitcher.OUTSIDE_POWER_CFG_BASE} ]]; then \n")
            cmd.append("busybox sed -i 's/\\r//' ${ModeSwitcher.OUTSIDE_POWER_CFG_BASE};\n")
            cmd.append("chmod 0777 ${ModeSwitcher.OUTSIDE_POWER_CFG_BASE};\n")
            cmd.append("fi;\n")
            KeepShellPublic.doCmdSync(cmd.toString())
        } catch (ex: Exception) {
        }
    }

    // 检查是否支持动态响应
    fun dynamicSupport(context: Context): Boolean {
        val cpuName = PlatformUtils().getCPUName()
        val names = context.assets.list(rootDir)
        if (names != null) {
            for (i in names.indices) {
                if (names[i].equals(cpuName)) {
                    return true
                }
            }
        }
        return false;
    }

    // 是否已经安装外部配置文件
    fun outsideConfigInstalled(): Boolean {
        return RootFile.fileNotEmpty(ModeSwitcher.OUTSIDE_POWER_CFG_PATH)
    }

    // 是否已经安装内部配置文件
    fun insideConfigInstalled(): Boolean {
        return File(FileWrite.getPrivateFilePath(Scene.context, "powercfg.sh")).exists()
    }
}
