package com.omarea.shell

import android.view.View

import com.omarea.shared.cmd_shellTools
import com.omarea.vboot.R

import java.io.File

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
        //return File("/cache/powercfg.sh").exists()
    }

    fun DynamicSupport(cpuName: String): Boolean {
        if (cpuName.contains("8998") || cpuName.contains("8996") || cpuName.contains("8992")) {
            return true
        }

        return false;
        //return File("/cache/powercfg.sh").exists()
    }
}
