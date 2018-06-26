package com.omarea.shared

import android.content.Context
import android.util.Log
import com.omarea.shell.Platform
import com.omarea.shell.SuDo
import com.omarea.shell.SysUtils
import java.io.File

class ConfigInstaller {
    fun installPowerConfig(context: Context, afterCmds: String, biCore: Boolean = false) {
        try {
            val powercfg = parseText(context, Platform().GetCPUName() + (if (biCore) "/powercfg-bigcore.sh" else "/powercfg-default.sh"))
            val powercfgBase = parseText(context, Platform().GetCPUName() + (if (biCore) "/init.qcom.post_boot-bigcore.sh" else "/init.qcom.post_boot-default.sh"))
            FileWrite.WritePrivateFile(powercfg, "powercfg.sh", context)
            FileWrite.WritePrivateFile(powercfgBase, "init.qcom.post_boot.sh", context)
            val cmd = StringBuilder()
                    .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg.sh")} ${Consts.POWER_CFG_PATH};")
                    .append("cp ${FileWrite.getPrivateFilePath(context, "init.qcom.post_boot.sh")} ${Consts.POWER_CFG_BASE};")
                    .append("chmod 0777 ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_BASE};")
                    .append(afterCmds)
            //SuDo(context).execCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + Consts.ExecuteConfig + "\n" + after)
            SuDo(context).execCmdSync(cmd.toString())
            ModeList(context).setCurrentPowercfg("")
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
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
            val file = File(Consts.POWER_CFG_PATH)
            val fileBase = File(Consts.POWER_CFG_PATH)
            if (file.exists() && (!file.canExecute() || !file.canRead())) {
                SuDo(context).execCmdSync("chmod 0775 ${Consts.POWER_CFG_PATH}")
            }
            if (fileBase.exists() && (!fileBase.canExecute() || !fileBase.canRead())) {
                SuDo(context).execCmdSync("chmod 0775 ${Consts.POWER_CFG_BASE}")
            }
            if (file.length() > 1024 * 1024) {
                return
            }
            if (fileBase.length() > 1024 * 1024) {
                return
            }
            val cmd = StringBuilder()
            if (file.exists()) {
                val powercfg = SysUtils.readOutputFromFile(Consts.POWER_CFG_PATH)
                if (powercfg.contains(Regex("\r\n")) || powercfg.contains(Regex("\r\t"))) {
                    FileWrite.WritePrivateFile(
                            powercfg
                                    .replace(Regex("\r\n"), "\n")
                                    .replace(Regex("\r\t"), "\t")
                                    .toByteArray(Charsets.UTF_8), "powercfg.sh",
                            context)
                    cmd
                            .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg.sh")} ${Consts.POWER_CFG_PATH};")
                            .append("chmod 0777 ${Consts.POWER_CFG_PATH};")
                }
            }
            if (fileBase.exists()) {
                val powercfgBase = SysUtils.readOutputFromFile(Consts.POWER_CFG_BASE)
                if (powercfgBase.contains(Regex("\r\n")) || powercfgBase.contains(Regex("\r\t"))) {
                    FileWrite.WritePrivateFile(
                            powercfgBase
                                    .replace(Regex("\r\n"), "\n")
                                    .replace(Regex("\r\t"), "\t")
                                    .toByteArray(Charsets.UTF_8), "init.qcom.post_boot.sh",
                            context)
                    cmd
                            .append("cp ${FileWrite.getPrivateFilePath(context, "init.qcom.post_boot.sh")} ${Consts.POWER_CFG_BASE};")
                            .append("chmod 0777 ${Consts.POWER_CFG_BASE};")
                }
            }
            if (cmd.length == 0)
                return
            SuDo(context).execCmdSync(cmd.toString())
        } catch (ex: Exception) {
            Log.e("script-parse", ex.message)
        }
    }
}
