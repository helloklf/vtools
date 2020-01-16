package com.omarea.shell_utils

import com.omarea.common.shell.KeepShell

class FstrimUtils(private val keepShell: KeepShell) {
    public fun run() {
        keepShell.doCmdSync("" +
                "fstrim /data" +
                "fstrim /data" +
                "fstrim /system" +
                "fstrim /system" +
                "fstrim /cache" +
                "fstrim /cache")
    }
}
