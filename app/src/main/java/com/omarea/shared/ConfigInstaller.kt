package com.omarea.shared

import android.content.Context
import android.util.Log
import com.omarea.shell.KeepShellSync
import com.omarea.shell.Platform
import java.nio.charset.Charset

class ConfigInstaller {
    fun installPowerConfig(context: Context, afterCmds: String, biCore: Boolean = false) {
        try {
            val powercfg = parseText(context, Platform().GetCPUName() + (if (biCore) "/powercfg-bigcore.sh" else "/powercfg-default.sh"))
            val powercfgBase = parseText(context, Platform().GetCPUName() + (if (biCore) "/powercfg-base-bigcore.sh" else "/powercfg-base-default.sh"))
            FileWrite.WritePrivateFile(powercfg, "powercfg.sh", context)
            FileWrite.WritePrivateFile(powercfgBase, "powercfg-base.sh", context)
            val cmd = StringBuilder()
                    .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg.sh")} ${Consts.POWER_CFG_PATH};")
                    .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg-base.sh")} ${Consts.POWER_CFG_BASE};")
                    .append("chmod 0777 ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_BASE};")
            //KeepShellSync.doCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + Consts.ExecuteConfig + "\n" + after)
            KeepShellSync.doCmdSync(cmd.toString())
            configCodeVerify(context)
            ModeList(context).setCurrentPowercfg("")
            KeepShellSync.doCmdSync(afterCmds)
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
        }
    }

    fun installPowerConfig(context: Context, powercfg: String): Boolean {
        try {
            FileWrite.WritePrivateFile(powercfg.replace("\r", "").toByteArray(Charset.forName("UTF-8")), "powercfg.sh", context)
            val cmd = StringBuilder()
                    .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg.sh")} ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_BASE};")
            //KeepShellSync.doCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + Consts.ExecuteConfig + "\n" + after)
            KeepShellSync.doCmdSync(cmd.toString())
            configCodeVerify(context)
            ModeList(context).setCurrentPowercfg("")
            return true
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
            return false
        }
    }


    //Dos转Unix，避免\r\n导致的脚本无法解析
    private fun parseText(context: Context, fileName: String): ByteArray {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open(fileName)
            val datas = ByteArray(2 * 1024 * 1024)
            //inputStream.available()
            val len = inputStream.read(datas)
            val codes = String(datas, 0, len).replace(Regex("\r\n"), "\n").replace(Regex("\r\t"), "\t")
            return codes.toByteArray(Charsets.UTF_8)
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
            return "".toByteArray()
        }
    }

    public fun configCodeVerify(context: Context) {
        try {
            val cmd = StringBuilder()
            cmd.append("if [[ -f ${Consts.POWER_CFG_PATH} ]]; then \n")
                cmd.append("chmod 0775 ${Consts.POWER_CFG_PATH};\n")
                cmd.append("busybox sed -i 's/^M//g' ${Consts.POWER_CFG_PATH};\n")
            cmd.append("fi;\n")
            cmd.append("if [[ -f ${Consts.POWER_CFG_BASE} ]]; then \n")
                cmd.append("chmod 0775 ${Consts.POWER_CFG_BASE};\n")
                cmd.append("busybox sed -i 's/^M//g' ${Consts.POWER_CFG_BASE};\n")
            cmd.append("fi;\n")
            KeepShellSync.doCmdSync(cmd.toString())
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
        }
    }
}
