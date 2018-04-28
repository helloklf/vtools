package com.omarea.vboot

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.omarea.shared.ConfigInstaller
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
            ConfigInstaller().installPowerConfig(this, after);
        }
    }
}
