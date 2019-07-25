package com.omarea.shell_utils

import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile
import java.io.File

/**
 * Created by Hello on 2017/11/01.
 */

class SwapUtils(private var context: Context) {
    private var swapfilePath: String = "/data/swapfile"
    private var swapControlScript = FileWrite.writePrivateShellFile("addin/swap_control.sh", "addin/swap_control.sh", context)
    private var zramControlScript = FileWrite.writePrivateShellFile("addin/zram_control.sh", "addin/zram_control.sh", context)

    val swapExists: Boolean
        get() {
            return RootFile.itemExists(swapfilePath)
        }

    // 当前已经激活的swap设备
    val currentSwapDevice: String
        get() {
            if (swapExists) {
                val ret = KernelProrp.getProp("/proc/swaps")
                val txt = ret.replace("\t\t", "\t").replace("\t", " ")
                if (txt.contains("/data/swapfile") || txt.contains("/swapfile")) {
                    return "/data/swapfile"
                } else {
                    val loopNumber = PropsUtils.getProp("vtools.swap.loop")
                    if (loopNumber.isNotEmpty() && loopNumber != "error" && txt.contains("loop" + loopNumber)) {
                        return "/dev/block/loop" + loopNumber
                    }
                }
            }
            return ""
        }

    val swapFileSize: Int
        get() {
            if (swapExists) {

                var size = 0L
                try {
                    size = KeepShellPublic.doCmdSync("ls -l /data/swapfile | awk '{ print \$5 }'").toLong()
                } catch (ex: Exception) {
                    try {
                        size = File("/data/swapfile").length()
                    } catch (ex: Exception) {

                    }
                }
                return (size / 1024 / 1024).toInt()
            }
            return 0
        }

    fun mkswap(size: Int) {
        val sb = StringBuilder()
        sb.append("swapoff $swapfilePath >/dev/null 2>&1;\n")
        sb.append("dd if=/dev/zero of=$swapfilePath bs=1048576 count=$size;\n")
        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    fun swapOn(hightPriority: Boolean, useLoop: Boolean = false) {
        val sb = StringBuilder()

        if (useLoop && swapControlScript != null) {
            sb.append("sh ")
            sb.append(swapControlScript)
            sb.append(" ")
            sb.append("enable_swap")
            if (hightPriority) {
                sb.append(" ")
                sb.append("32760")
            }
        } else {
            // sb.append("echo 3 > /sys/block/zram0/max_comp_streams\n")
            sb.append("mkswap $swapfilePath;\n")
            if (hightPriority) {
                sb.append("swapon $swapfilePath -p 32760\n")
            } else {
                sb.append("swapon $swapfilePath\n")
            }
        }

        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    fun swapOff() {
        val sb = StringBuilder("sync\necho 3 > /proc/sys/vm/drop_caches\n")

        if (currentSwapDevice.contains("loop") && swapControlScript != null) {
            sb.append("sh ")
            sb.append(swapControlScript)
            sb.append(" ")
            sb.append("diable_swap")
        } else {
            sb.append("busybox swapoff $swapfilePath > /dev/null 2>&1")
        }

        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    fun swapDelete() {
        val sb = StringBuilder("sync\necho 3 > /proc/sys/vm/drop_caches\n")

        if (currentSwapDevice.contains("loop") && swapControlScript != null) {
            sb.append("sh ")
            sb.append(swapControlScript)
            sb.append(" ")
            sb.append("diable_swap")
        } else {
            sb.append("busybox swapoff $swapfilePath > /dev/null 2>&1")
        }
        sb.append("\nrm -f $swapfilePath;")

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
            sb.append("echo 4 > /sys/block/zram0/max_comp_streams\n")
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
        set(value) {
            KernelProrp.setProp("/sys/block/zram0/comp_algorithm", value)
        }
}