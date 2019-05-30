package com.omarea.shared

import android.content.Context
import android.util.Log
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.shell.Platform
import java.nio.charset.Charset

class ConfigInstaller {
    fun installPowerConfig(context: Context, afterCmds: String = "", biCore: Boolean = false) {
        try {
            val powercfg = com.omarea.common.shared.FileWrite.writePrivateShellFile(Platform().getCPUName() + (if (biCore) "/powercfg-bigcore.sh" else "/powercfg-default.sh"), "powercfg.sh", context)
            val powercfgBase = com.omarea.common.shared.FileWrite.writePrivateShellFile(Platform().getCPUName() + (if (biCore) "/powercfg-base-bigcore.sh" else "/powercfg-base-default.sh"), "powercfg-base.sh", context)

            val cmd = StringBuilder()
                    .append("cp ${powercfg} ${ModeList.POWER_CFG_PATH};")
                    .append("cp ${powercfgBase} ${ModeList.POWER_CFG_BASE};")
                    .append("chmod 0777 ${ModeList.POWER_CFG_PATH};")
                    .append("chmod 0777 ${ModeList.POWER_CFG_BASE};")
            //KeepShellPublic.doCmdSync(CommonCmds.InstallPowerToggleConfigToCache + "\n\n" + CommonCmds.ExecuteConfig + "\n" + after)
            KeepShellPublic.doCmdSync(cmd.toString())
            configCodeVerify()
            ModeList().setCurrentPowercfg("")
            if (!afterCmds.isEmpty()) {
                KeepShellPublic.doCmdSync(afterCmds)
            }
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
        }
    }

    fun installPowerConfigByText(context: Context, powercfg: String, powercfgBase: String = "#!/system/bin/sh"): Boolean {
        try {
            com.omarea.common.shared.FileWrite.writePrivateFile(powercfg.replace("\r", "").toByteArray(Charset.forName("UTF-8")), "powercfg.sh", context)
            com.omarea.common.shared.FileWrite.writePrivateFile(powercfgBase.replace("\r", "").toByteArray(Charset.forName("UTF-8")), "powercfg-base.sh", context)
            val cmd = StringBuilder()
                    .append("cp ${com.omarea.common.shared.FileWrite.getPrivateFilePath(context, "powercfg.sh")} ${ModeList.POWER_CFG_PATH};")
                    .append("cp ${com.omarea.common.shared.FileWrite.getPrivateFilePath(context, "powercfg-base.sh")} ${ModeList.POWER_CFG_BASE};")
                    .append("chmod 0777 ${ModeList.POWER_CFG_PATH};")
                    .append("chmod 0777 ${ModeList.POWER_CFG_BASE};")
            //KeepShellPublic.doCmdSync(CommonCmds.InstallPowerToggleConfigToCache + "\n\n" + CommonCmds.ExecuteConfig + "\n" + after)
            KeepShellPublic.doCmdSync(cmd.toString())
            ModeList().setCurrentPowercfg("")
            return true
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
            return false
        }
    }

    fun configCodeVerify() {
        try {
            val cmd = StringBuilder()
            cmd.append("if [[ -f ${ModeList.POWER_CFG_PATH} ]]; then \n")
            cmd.append("busybox sed -i 's/\\r//' ${ModeList.POWER_CFG_PATH};\n")
            cmd.append("chmod 0775 ${ModeList.POWER_CFG_PATH};\n")
            cmd.append("fi;\n")
            cmd.append("if [[ -f ${ModeList.POWER_CFG_BASE} ]]; then \n")
            cmd.append("busybox sed -i 's/\\r//' ${ModeList.POWER_CFG_BASE};\n")
            cmd.append("chmod 0777 ${ModeList.POWER_CFG_BASE};\n")
            cmd.append("fi;\n")
            KeepShellPublic.doCmdSync(cmd.toString())
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
        }
    }

    fun dynamicSupport(context: Context): Boolean {
        val cpuName = Platform().getCPUName()
        val names = context.assets.list("")
        for (i in names.indices) {
            if (names[i].equals(cpuName)) {
                return true
            }
        }
        return false;
    }

    fun configInstalled(): Boolean {
        return RootFile.fileNotEmpty(ModeList.POWER_CFG_PATH)
    }
}
