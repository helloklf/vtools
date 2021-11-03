package com.omarea.library.shell

import com.omarea.common.shell.KeepShellPublic

class DDRUtils() {
    fun getDDRType(): Int {
        val result = KeepShellPublic.doCmdSync("od -An -tx /proc/device-tree/memory/ddr_device_type")
        when (result) {
            "05" -> {
                return 3
            }
            "07" -> {
                return 4
            }
            "08" -> {
                return 5
            }
        }
        return 0
    }
}
