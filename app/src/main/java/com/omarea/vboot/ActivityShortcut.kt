package com.omarea.vboot

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shared.FileWrite
import com.omarea.shell.Platform
import com.omarea.shell.SuDo
import java.io.File

class ActivityShortcut : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Platform().dynamicSupport(this)) {
            val action = intent.action
            when (action) {
                "powersave" -> {
                    installConfig(Consts.TogglePowersaveMode)
                    Toast.makeText(this, getString(R.string.powersave), Toast.LENGTH_SHORT).show()
                }
                "balance" -> {
                    installConfig(Consts.ToggleDefaultMode)
                    Toast.makeText(this, getString(R.string.balance), Toast.LENGTH_SHORT).show()
                }
                "performance" -> {
                    installConfig(Consts.ToggleGameMode)
                    Toast.makeText(this, getString(R.string.performance), Toast.LENGTH_SHORT).show()
                }
                "fast" -> {
                    installConfig(Consts.ToggleFastMode)
                    Toast.makeText(this, getString(R.string.fast), Toast.LENGTH_SHORT).show()
                }
                else -> {  }
            }
        } else {
            Toast.makeText(this, getString(R.string.device_unsupport), Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun installConfig(after: String) {
        if (File(Consts.POWER_CFG_PATH).exists()) {
            SuDo(this).execCmdSync(after)
        } else {
            //TODO：选取配置
            FileWrite.WritePrivateFile(this.assets, Platform().GetCPUName() + "/powercfg-default.sh", "powercfg.sh", this)
            FileWrite.WritePrivateFile(this.assets, Platform().GetCPUName() + "/init.qcom.post_boot-default", "init.qcom.post_boot.sh", this)
            val cmd = StringBuilder()
                    .append("cp ${FileWrite.getPrivateFilePath(this, "powercfg.sh")} ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_BASE};")
                    .append(after)
            SuDo(this).execCmdSync(cmd.toString())
        }
    }
}
