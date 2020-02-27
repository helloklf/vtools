package com.omarea.shell_utils

import com.omarea.common.shell.KeepShell

class FstrimUtils(private val keepShell: KeepShell) {
    public fun run() {
        keepShell.doCmdSync("" +
                "fstrim /data\n" +
                "fstrim /data\n" +
                "fstrim /system\n" +
                "fstrim /system\n" +
                "fstrim /cache\n" +
                "fstrim /cache\n")
    }
}
