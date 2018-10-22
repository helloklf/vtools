package com.omarea.shared

import android.content.Context
import android.util.Log
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.Platform
import java.nio.charset.Charset

class ConfigInstaller {
    fun installPowerConfig(context: Context, afterCmds: String, biCore: Boolean = false) {
        try {
            val powercfg = FileWrite.writePrivateShellFile(Platform().getCPUName() + (if (biCore) "/powercfg-bigcore.sh" else "/powercfg-default.sh"), "powercfg.sh", context)
            val powercfgBase = FileWrite.writePrivateShellFile(Platform().getCPUName() + (if (biCore) "/powercfg-base-bigcore.sh" else "/powercfg-base-default.sh"), "powercfg-base.sh", context)

            val cmd = StringBuilder()
                    .append("cp ${powercfg} ${CommonCmds.POWER_CFG_PATH};")
                    .append("cp ${powercfgBase} ${CommonCmds.POWER_CFG_BASE};")
                    .append("chmod 0777 ${CommonCmds.POWER_CFG_PATH};")
                    .append("chmod 0777 ${CommonCmds.POWER_CFG_BASE};")
            //KeepShellPublic.doCmdSync(CommonCmds.InstallPowerToggleConfigToCache + "\n\n" + CommonCmds.ExecuteConfig + "\n" + after)
            KeepShellPublic.doCmdSync(cmd.toString())
            configCodeVerify()
            ModeList(context).setCurrentPowercfg("")
            KeepShellPublic.doCmdSync(afterCmds)
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
        }
    }

    fun installPowerConfigByText(context: Context, powercfg: String): Boolean {
        try {
            FileWrite.writePrivateFile(powercfg.replace("\r", "").toByteArray(Charset.forName("UTF-8")), "powercfg.sh", context)
            val cmd = StringBuilder()
                    .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg.sh")} ${CommonCmds.POWER_CFG_PATH};")
                    .append("chmod 0777 ${CommonCmds.POWER_CFG_PATH};")
                    .append("chmod 0777 ${CommonCmds.POWER_CFG_BASE};")
            //KeepShellPublic.doCmdSync(CommonCmds.InstallPowerToggleConfigToCache + "\n\n" + CommonCmds.ExecuteConfig + "\n" + after)
            KeepShellPublic.doCmdSync(cmd.toString())
            configCodeVerify()
            ModeList(context).setCurrentPowercfg("")
            return true
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
            return false
        }
    }

    fun configCodeVerify() {
        try {
            val cmd = StringBuilder()
            cmd.append("if [[ -f ${CommonCmds.POWER_CFG_PATH} ]]; then \n")
                cmd.append("busybox sed -i 's/\\r//' ${CommonCmds.POWER_CFG_PATH};\n")
                cmd.append("chmod 0775 ${CommonCmds.POWER_CFG_PATH};\n")
            cmd.append("fi;\n")
            cmd.append("if [[ -f ${CommonCmds.POWER_CFG_BASE} ]]; then \n")
                cmd.append("busybox sed -i 's/\\r//' ${CommonCmds.POWER_CFG_BASE};\n")
                cmd.append("chmod 0775 ${CommonCmds.POWER_CFG_BASE};\n")
            cmd.append("fi;\n")
            KeepShellPublic.doCmdSync(cmd.toString())
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
        }
    }
}
