package com.omarea.vboot

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.omarea.shared.AppShared
import com.omarea.shared.ConfigInfo
import com.omarea.shared.Consts
import com.omarea.shared.cmd_shellTools

class shortcuts : AppCompatActivity() {

    lateinit internal var cmd_shellTools: cmd_shellTools

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shortcuts)
        val action = intent.action
        when (action) {
            "clear" -> {
                cmd_shellTools = cmd_shellTools(this, null)
                cmd_shellTools.DoCmd(Consts.ClearCache)
            }
            "powersave" -> {
                cmd_shellTools = cmd_shellTools(this, null)
                InstallPowerToggleConfig()
                cmd_shellTools.DoCmd(Consts.TogglePowersaveMode)
            }
            "defaultmode" -> {
                cmd_shellTools = cmd_shellTools(this, null)
                InstallPowerToggleConfig()
                cmd_shellTools.DoCmd(Consts.ToggleDefaultMode)
            }
            "gamemode" -> {
                cmd_shellTools = cmd_shellTools(this, null)
                InstallPowerToggleConfig()
                cmd_shellTools.DoCmd(Consts.ToggleGameMode)
            }
            "fastmode" -> {
                cmd_shellTools = cmd_shellTools(this, null)
                InstallPowerToggleConfig()
                cmd_shellTools.DoCmd(Consts.ToggleFastMode)
            }
            "android.intent.action.VIEW" -> {
            }
            else -> {
            }
        }
        Toast.makeText(this, action + " 操作完成！", Toast.LENGTH_SHORT).show()
        finish()
    }

    internal fun InstallPowerToggleConfig() {

        if (ConfigInfo.getConfigInfo().UseBigCore)
            AppShared.WriteFile(assets, ConfigInfo.getConfigInfo().CPUName + "/powercfg-bigcore.sh", "powercfg.sh")
        else
            AppShared.WriteFile(assets, ConfigInfo.getConfigInfo().CPUName + "/powercfg-default.sh", "powercfg.sh")
        cmd_shellTools.DoCmd(Consts.InstallPowerToggleConfigToCache)
    }
}
