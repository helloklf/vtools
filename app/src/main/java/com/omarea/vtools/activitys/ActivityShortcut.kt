package com.omarea.vtools.activitys

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.shared.ConfigInstaller
import com.omarea.shared.ModeList
import com.omarea.shell.Platform
import com.omarea.shell.RootFile
import com.omarea.vtools.R
import java.io.File

class ActivityShortcut : Activity() {
    private var modeList = ModeList()
    val configInstaller = ConfigInstaller()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (configInstaller.dynamicSupport(this.applicationContext) || configInstaller.configInstalled()) {
            val action = intent.action
            if (action == "powersave" || action == "balance" || action == "performance" || action == "fast") {
                installConfig(action)
                Toast.makeText(applicationContext, ModeList.getModName(action), Toast.LENGTH_SHORT).show()
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
            ConfigInstaller().installPowerConfig(this);
        }
        modeList.executePowercfgMode(action, packageName)
    }
}
