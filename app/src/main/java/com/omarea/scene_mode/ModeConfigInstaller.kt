package com.omarea.scene_mode

import android.content.Context
import android.util.Log
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.shell_utils.PlatformUtils
import java.nio.charset.Charset

class ModeConfigInstaller {
    val rootDir = "powercfg"
    fun getPowerCfgDir(): String {
        return rootDir + "/" + PlatformUtils().getCPUName()
    }

    fun installPowerConfig(context: Context, afterCmds: String = "", biCore: Boolean = false) {
        try {
            val powercfg = FileWrite.writePrivateShellFile(getPowerCfgDir() + (if (biCore) "/powercfg-bigcore.sh" else "/powercfg-default.sh"), "powercfg.sh", context)
            val powercfgBase = FileWrite.writePrivateShellFile(getPowerCfgDir() + (if (biCore) "/powercfg-base-bigcore.sh" else "/powercfg-base-default.sh"), "powercfg-base.sh", context)

            val cmd = StringBuilder()
                    .append("cp ${powercfg} ${ModeSwitcher.POWER_CFG_PATH};")
                    .append("cp ${powercfgBase} ${ModeSwitcher.POWER_CFG_BASE};")
                    .append("chmod 0777 ${ModeSwitcher.POWER_CFG_PATH};")
                    .append("chmod 0777 ${ModeSwitcher.POWER_CFG_BASE};")
            //KeepShellPublic.doCmdSync(CommonCmds.InstallPowerToggleConfigToCache + "\n\n" + CommonCmds.ExecuteConfig + "\n" + after)
            KeepShellPublic.doCmdSync(cmd.toString())
            configCodeVerify()
            ModeSwitcher().setCurrentPowercfg("")
            if (!afterCmds.isEmpty()) {
                KeepShellPublic.doCmdSync(afterCmds)
            }
        } catch (ex: Exception) {
            Log.e("script-parse", "" + ex.message)
        }
    }

    fun installPowerConfigByText(context: Context, powercfg: String, powercfgBase: String = "#!/system/bin/sh"): Boolean {
        try {
            FileWrite.writePrivateFile(powercfg.replace("\r", "")
                    .toByteArray(Charset.forName("UTF-8")), "powercfg.sh", context)
            FileWrite.writePrivateFile(powercfgBase.replace("\r", "")
                    .toByteArray(Charset.forName("UTF-8")), "powercfg-base.sh", context)
            val cmd = StringBuilder()
                    .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg.sh")} ${ModeSwitcher.POWER_CFG_PATH};")
                    .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg-base.sh")} ${ModeSwitcher.POWER_CFG_BASE};")
                    .append("chmod 0777 ${ModeSwitcher.POWER_CFG_PATH};")
                    .append("chmod 0777 ${ModeSwitcher.POWER_CFG_BASE};")
            //KeepShellPublic.doCmdSync(CommonCmds.InstallPowerToggleConfigToCache + "\n\n" + CommonCmds.ExecuteConfig + "\n" + after)
            KeepShellPublic.doCmdSync(cmd.toString())
            ModeSwitcher().setCurrentPowercfg("")
            return true
        } catch (ex: Exception) {
            Log.e("script-parse", "" + ex.message)
            return false
        }
    }

    fun configCodeVerify() {
        try {
            val cmd = StringBuilder()
            cmd.append("if [[ -f ${ModeSwitcher.POWER_CFG_PATH} ]]; then \n")
            cmd.append("busybox sed -i 's/\\r//' ${ModeSwitcher.POWER_CFG_PATH};\n")
            cmd.append("chmod 0775 ${ModeSwitcher.POWER_CFG_PATH};\n")
            cmd.append("fi;\n")
            cmd.append("if [[ -f ${ModeSwitcher.POWER_CFG_BASE} ]]; then \n")
            cmd.append("busybox sed -i 's/\\r//' ${ModeSwitcher.POWER_CFG_BASE};\n")
            cmd.append("chmod 0777 ${ModeSwitcher.POWER_CFG_BASE};\n")
            cmd.append("fi;\n")
            KeepShellPublic.doCmdSync(cmd.toString())
        } catch (ex: Exception) {
            Log.e("script-parse", "" + ex.message)
        }
    }

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

    fun configInstalled(): Boolean {
        return RootFile.fileNotEmpty(ModeSwitcher.POWER_CFG_PATH)
    }
}
