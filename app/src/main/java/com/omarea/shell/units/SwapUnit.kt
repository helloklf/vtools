package com.omarea.shell.units

import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile

/**
 * Created by Hello on 2017/11/01.
 */

class SwapUnit() {
    private var swapfilePath: String = "/data/swapfile"

    val swapExists: Boolean
        get() {
            return RootFile.itemExists("/data/swapfile")
        }

    fun mkswap(size: Int) {
        val sb = StringBuilder()
        sb.append("swapoff $swapfilePath >/dev/null 2>&1;\n")
        sb.append("dd if=/dev/zero of=$swapfilePath bs=1048576 count=$size;\n")
        sb.append("mkswap $swapfilePath;\n")
        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    fun swapOn(hightPriority: Boolean) {
        val sb = StringBuilder()
        sb.append("echo 3 > /sys/block/zram0/max_comp_streams\n")
        if (hightPriority) {
            sb.append("swapon $swapfilePath -p 32767\n")
            //sb.append("swapoff /dev/block/zram0\n")
        } else {
            sb.append("swapon $swapfilePath\n")
        }
        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    fun swapOff() {
        val keepShell = KeepShell()
        keepShell.doCmdSync("echo 3 > /sys/block/zram0/max_comp_streams\n" +
                "sync\n" +
                "echo 3 > /proc/sys/vm/drop_caches\n" +
                "busybox swapoff /data/swapfile > /dev/null 2>&1")
        keepShell.tryExit()
    }

    fun swapDelete() {
        val sb = StringBuilder()
        sb.append("echo 3 > /sys/block/zram0/max_comp_streams;")
        sb.append("sync\necho 3 > /proc/sys/vm/drop_caches\nswapoff /data/swapfile >/dev/null 2>&1;")
        sb.append("rm -f /data/swapfile;")

        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    val zramSupport: Boolean
        get() {
            return KeepShellPublic.doCmdSync("if [[ -e /dev/block/zram0 ]]; then echo 1; else echo 0; fi;").equals("1")
        }

    val zramEnabled: Boolean
        get() {
            return KeepShellPublic.doCmdSync("cat /proc/swaps | grep /block/zram0").contains("/block/zram0")
        }

    fun resizeZram(sizeVal: Int, algorithm: String = "") {
        val keepShell = KeepShell()
        val currentSize = keepShell.doCmdSync("cat /sys/block/zram0/disksize")
        if (currentSize != "" + (sizeVal * 1024 * 1024L) || (algorithm.isNotEmpty() && algorithm != compAlgorithm)) {
            val sb = StringBuilder()
            sb.append("echo 3 > /sys/block/zram0/max_comp_streams\n")
            sb.append("sync\n")
            sb.append("echo 3 > /proc/sys/vm/drop_caches\n")
            sb.append("swapoff /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("echo 1 > /sys/block/zram0/reset\n")

            if (algorithm.isNotEmpty()) {
                sb.append("echo \"$algorithm\" > /sys/block/zram0/comp_algorithm\n")
            }

            if (sizeVal > 2047) {
                sb.append("echo " + sizeVal + "M > /sys/block/zram0/disksize\n")
            } else {
                sb.append("echo " + (sizeVal * 1024 * 1024L) + " > /sys/block/zram0/disksize\n")
            }

            sb.append("echo 3 > /sys/block/zram0/max_comp_streams\n")
            sb.append("mkswap /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("swapon /dev/block/zram0 >/dev/null 2>&1\n")
            keepShell.doCmdSync(sb.toString())
        }

        keepShell.tryExit()
    }

    /**
     * 获取可用的ZRAM压缩算法
     */
    val compAlgorithmOptions: Array<String>
        get() {
            val compAlgorithmItems = KernelProrp.getProp("/sys/block/zram0/comp_algorithm").split(" ")
            return compAlgorithmItems.map {
                it.replace("[", "").replace("]", "")
            }.toTypedArray()
        }

    /**
     * 获取当前的ZRAM压缩算法
     */
    var compAlgorithm: String
        get() {
            val compAlgorithmItems = KernelProrp.getProp("/sys/block/zram0/comp_algorithm").split(" ")
            val result = compAlgorithmItems.find {
                it.startsWith("[") && it.endsWith("]")
            }
            if (result != null) {
                return result.replace("[", "").replace("]", "").trim()
            }
            return ""
        }
        set (value) {
            KernelProrp.setProp("/sys/block/zram0/comp_algorithm", value)
        }
}