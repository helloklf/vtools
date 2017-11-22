package com.omarea.shell

import android.content.Context

/**
 * Created by helloklf on 2017/6/3.
 */

class DynamicConfig {
    fun DynamicSupport(): Boolean {
        val cpuName = Platform().GetCPUName()
        if (cpuName.contains("8998") || cpuName.contains("8996") || cpuName.contains("8992")) {
            return true
        }

        return false;
    }

    fun DynamicSupport(context: Context): Boolean {
        var cpuName = Platform().GetCPUName()
        var names = context.assets.list("")
        for (i in names.indices) {
            if (names[i].equals(cpuName)) {
                return true
            }
        }
        return false;
    }
}
