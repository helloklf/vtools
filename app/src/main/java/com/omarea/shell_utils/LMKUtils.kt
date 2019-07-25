package com.omarea.shell_utils

import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic

/**
 * Created by Hello on 2018/08/05.
 */

class LMKUtils {
    fun autoSetLMK(totalRamBytes: Long, keepShell: KeepShell? = null) {
        //echo "0,100,200,300,900,906" > /sys/module/lowmemorykiller/parameters/adj
        //echo "14746,18432,22118,25805,40000,55000" > /sys/module/lowmemorykiller/parameters/minfree
        //echo "18432,23040,27648,32256,56250,81250" > /sys/module/lowmemorykiller/parameters/minfree

        var ratio = 1f

        // 8GB
        if (totalRamBytes > (6144 * 1024 * 1024L)) {
            ratio = 2f
        }
        // 6GB
        else if (totalRamBytes > (4096 * 1024 * 1024L)) {
            ratio = 1.5f
        }
        // 4GB
        else if (totalRamBytes > (3072 * 1024 * 1024L)) {
            ratio = 1.2f
        }
        // 3GB
        else if (totalRamBytes > (2048 * 1024 * 1024L)) {
            ratio = 1f
        }
        // 2GB
        else if (totalRamBytes > 1024 * 1024 * 1024) {
            ratio = 0.5f
        }
        // 1GB
        else if (totalRamBytes > 1024 * 1024 * 1024) {
            ratio = 0.25f
        }
        // < 1GB (这破手机还用毛啊！！！)
        else {
            ratio = 0.2f
        }

        val foregroundApp = (40 * 1024 / 4 * ratio).toInt()
        val visibleApp = (64 * 1024 / 4 * ratio).toInt()
        val serviceApp = (72 * 1024 / 4 * ratio).toInt()
        val hideApp = (80 * 1024 / 4 * ratio).toInt()
        val contentProviderApp = (120 * 1024 / 4 * ratio).toInt()
        val emptyApp = (130 * 1024 / 4 * ratio).toInt()

        val minfree = "/sys/module/lowmemorykiller/parameters/minfree"
        val adaptiveLMK = "/sys/module/lowmemorykiller/parameters/enable_adaptive_lmk"
        val cmds = StringBuilder()
        cmds.append("chmod 666 $minfree\n")
        cmds.append("echo \"$foregroundApp,$visibleApp,$serviceApp,$hideApp,$contentProviderApp,$emptyApp\" > $minfree\n")
        cmds.append("chmod 666 $adaptiveLMK\n")
        cmds.append("echo 0 > $adaptiveLMK\n")
        if (keepShell == null) {
            KeepShellPublic.doCmdSync(cmds.toString())
        } else {
            keepShell.doCmdSync(cmds.toString())
        }
    }
}
