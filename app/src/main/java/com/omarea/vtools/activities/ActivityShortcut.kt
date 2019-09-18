package com.omarea.vtools.activities

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.omarea.scene_mode.ModeConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.vtools.R

class ActivityShortcut : Activity() {
    private var modeList = ModeSwitcher()
    val configInstaller = ModeConfigInstaller()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (configInstaller.dynamicSupport(this.applicationContext) || configInstaller.configInstalled()) {
            val action = intent.action
            if (action == "powersave" || action == "balance" || action == "performance" || action == "fast") {
                installConfig(action)
                Toast.makeText(applicationContext, ModeSwitcher.getModName(action), Toast.LENGTH_SHORT).show()
            } else if (!intent.getPackage().isNullOrEmpty()) {
                startActivity(intent)
            }
        } else {
            Toast.makeText(applicationContext, getString(R.string.device_unsupport), Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun installConfig(action: String) {
        if (!configInstaller.configInstalled()) {
            ModeConfigInstaller().installPowerConfig(this);
        }
        modeList.executePowercfgMode(action, packageName)
    }
}
