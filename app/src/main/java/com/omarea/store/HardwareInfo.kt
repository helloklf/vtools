package com.omarea.store

import android.content.Context
import com.omarea.Scene
import com.omarea.library.shell.DDRUtils
import com.omarea.library.shell.PlatformUtils

class HardwareInfo {
    private val config = Scene.context.getSharedPreferences("HardwareInfo", Context.MODE_PRIVATE)

    public fun getPlatform (): String {
        if (!config.contains("platform")) {
            val result = PlatformUtils().getCPUName()
            if (result.isNotEmpty() && result != "error") {
                config.edit().putString("platform", result).apply()
            }
        }
        return config.getString("platform", "")!!
    }

    public fun getDDRType (): Int {
        if (!config.contains("ddr_type")) {
            val result = DDRUtils().getDDRType()
            if (result > 0) {
                config.edit().putInt("ddr_type", result).apply()
            }
        }
        return config.getInt("ddr_type", -1)
    }

    public fun preInit() {
        this.getPlatform()
        this.getDDRType()
    }
}