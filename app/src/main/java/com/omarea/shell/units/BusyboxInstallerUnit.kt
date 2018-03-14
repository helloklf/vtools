package com.omarea.shell.units

import com.omarea.shared.cmd_shellTools

import java.io.DataOutputStream
import java.io.IOException

/**
 * Created by Hello on 2017/11/01.
 */

class BusyboxInstallerUnit {
    //安装Shell工具
    fun InstallShellTools() {
        InstallShellToolsThread().start()
    }

    internal inner class InstallShellToolsThread : Thread() {
        override fun run() {
            try {
                val process = Runtime.getRuntime().exec("su")
                val out = DataOutputStream(process.outputStream)
                out.writeBytes("busybox --install /system/xbin\n")
                out.writeBytes("\n")
                out.writeBytes("exit\n")
                out.writeBytes("exit\n")
                out.flush()
                process.waitFor()
                process.destroy()
            } catch (e: Exception) {

            }

        }
    }
}
