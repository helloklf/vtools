package com.omarea.shared

import android.content.Context
import com.omarea.shell.Platform
import com.omarea.shell.SuDo

class ConfigInstaller {
    fun installPowerConfig(context: Context, afterCmds: String) {
        FileWrite.WritePrivateFile(context.assets, Platform().GetCPUName() + "/powercfg-default.sh", "powercfg.sh", context)
        FileWrite.WritePrivateFile(context.assets, Platform().GetCPUName() + "/init.qcom.post_boot-default.sh", "init.qcom.post_boot.sh", context)
        val cmd = StringBuilder()
                .append("cp ${FileWrite.getPrivateFilePath(context, "powercfg.sh")} ${Consts.POWER_CFG_PATH};")
                .append("cp ${FileWrite.getPrivateFilePath(context, "init.qcom.post_boot.sh")} ${Consts.POWER_CFG_BASE};")
                .append("chmod 0777 ${Consts.POWER_CFG_PATH};")
                .append("chmod 0777 ${Consts.POWER_CFG_BASE};")
                .append(afterCmds)
        //SuDo(context).execCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + Consts.ExecuteConfig + "\n" + after)
        SuDo(context).execCmdSync(cmd.toString())
    }
}
