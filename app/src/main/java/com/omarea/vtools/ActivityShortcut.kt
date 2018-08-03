package com.omarea.vtools

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.omarea.shared.ConfigInstaller
import com.omarea.shared.CommonCmds
import com.omarea.shared.ModeList
import com.omarea.shell.Platform
import java.io.File

class ActivityShortcut : Activity() {
    private var modeList = ModeList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Platform().dynamicSupport(this) || File(CommonCmds.POWER_CFG_PATH).exists()) {
            val action = intent.action
            if (action != null && !action.isNullOrEmpty()) {
                installConfig(action)
                Toast.makeText(applicationContext, ModeList.getModName(action), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(applicationContext, getString(R.string.device_unsupport), Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun installConfig(action: String) {
        val stringBuilder = StringBuilder()
        stringBuilder.append(String.format(CommonCmds.ToggleMode, action))

        if (File(CommonCmds.POWER_CFG_PATH).exists()) {
            modeList.executePowercfgMode(action, packageName)
        } else {
            ConfigInstaller().installPowerConfig(this, stringBuilder.toString());
        }
    }
}
