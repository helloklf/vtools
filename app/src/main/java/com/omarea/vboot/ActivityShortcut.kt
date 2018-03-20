package com.omarea.vboot

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.omarea.shared.AppShared
import com.omarea.shared.Consts
import com.omarea.shell.DynamicConfig
import com.omarea.shell.Platform
import com.omarea.shell.SuDo
import java.io.File

class ActivityShortcut : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_shortcut)
        if (DynamicConfig().DynamicSupport(this)) {
            val action = intent.action
            when (action) {
                "powersave" -> {
                    installConfig(Consts.TogglePowersaveMode)
                    Toast.makeText(this, "省电模式", Toast.LENGTH_SHORT).show()
                }
                "balance" -> {
                    installConfig(Consts.ToggleDefaultMode)
                    Toast.makeText(this, "均衡模式", Toast.LENGTH_SHORT).show()
                }
                "performance" -> {
                    installConfig(Consts.ToggleGameMode)
                    Toast.makeText(this, "性能模式", Toast.LENGTH_SHORT).show()
                }
                "fast" -> {
                    installConfig(Consts.ToggleFastMode)
                    Toast.makeText(this, "极速模式", Toast.LENGTH_SHORT).show()
                }
                else -> {  }
            }
        } else {
            Toast.makeText(this, "暂不支持您当前设备！", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun installConfig(after: String) {
        if (File(Consts.POWER_CFG_PATH).exists()) {
            SuDo(this).execCmdSync(after)
            //SuDo(context).execCmdSync(Consts.ExecuteConfig + "\n" + after)
        } else {
            //TODO：选取配置
            AppShared.WritePrivateFile(this.assets, Platform().GetCPUName() + "/powercfg-default.sh", "powercfg.sh", this)
            val cmd = StringBuilder()
                    .append("cp ${AppShared.getPrivateFilePath(this, "powercfg.sh")} ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_PATH};")
                    .append(after)
            //SuDo(context).execCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + Consts.ExecuteConfig + "\n" + after)
            SuDo(this).execCmdSync(cmd.toString())
        }
    }
}
