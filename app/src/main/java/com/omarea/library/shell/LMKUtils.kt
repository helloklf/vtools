package com.omarea.library.shell

import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile

/**
 * Created by Hello on 2018/08/05.
 * LMK设置
 */

class LMKUtils {
    private val path = "/sys/module/lowmemorykiller/parameters/minfree"
    fun supported(): Boolean {
        return RootFile.fileExists(path)
    }

    fun getCurrent(): String {
        return KernelProrp.getProp(path)
    }

    fun autoSetLMK(totalRamBytes: Long, keepShell: KeepShell? = null) {
        //echo "0,100,200,300,900,906" > /sys/module/lowmemorykiller/parameters/adj
        //echo "14746,18432,22118,25805,40000,55000" > /sys/module/lowmemorykiller/parameters/minfree
        //echo "18432,23040,27648,32256,56250,81250" > /sys/module/lowmemorykiller/parameters/minfree

        val ratio = when {
            totalRamBytes > (8192 * 1024 * 1024L) -> 2f
            // 8GB
            totalRamBytes > (6144 * 1024 * 1024L) -> 1.5f
            // 6GB
            totalRamBytes > (4096 * 1024 * 1024L) -> 1f
            // 4GB
            totalRamBytes > (3072 * 1024 * 1024L) -> 1f
            // 3GB
            totalRamBytes > (2048 * 1024 * 1024L) -> 0.7f
            // 2GB
            totalRamBytes > 1024 * 1024 * 1024 -> 0.5f
            // 1GB
            totalRamBytes > 1024 * 1024 * 1024 -> 0.25f
            // < 1GB (这破手机还用毛啊！！！)
            else -> 0.2f
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
