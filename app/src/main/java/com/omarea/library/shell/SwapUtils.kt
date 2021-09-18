package com.omarea.library.shell

import android.content.Context
import android.util.Log
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile
import com.omarea.model.ZramWriteBackStat
import java.io.File

/**
 * Created by Hello on 2017/11/01.
 */

class SwapUtils(private val context: Context) {


    private var swapfilePath: String = "/data/swapfile"
    private var swapControlScript = FileWrite.writePrivateShellFile("addin/swap_control.sh", "addin/swap_control.sh", context)
    private var swapForceKswapdScript:String? = null
    // private var zramControlScript = FileWrite.writePrivateShellFile("addin/zram_control.sh", "addin/zram_control.sh", context)

    // 是否已创建swapfile文件
    val swapExists: Boolean
        get() {
            return RootFile.itemExists(swapfilePath)
        }

    // 当前已由Scene激活的swap
    val sceneSwaps: String
        get() {
            if (swapExists) {
                val ret = KernelProrp.getProp("/proc/swaps")
                val txt = ret.replace("\t\t", "\t").replace("\t", " ")
                if (txt.contains("/data/swapfile") || txt.contains("/swapfile")) {
                    return "/data/swapfile"
                } else {
                    val loopName = PropsUtils.getProp("vtools.swap.loop").split("/").lastOrNull()
                    if (loopName != null && loopName != "error" && txt.contains(loopName)) {
                        return loopName
                    }
                }
            }
            return ""
        }

    // 获取当前swap的大小
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

    // 创建swap
    fun mkswap(size: Int) {
        val sb = StringBuilder()
        sb.append("swapoff $swapfilePath >/dev/null 2>&1;\n")
        sb.append("dd if=/dev/zero of=$swapfilePath bs=1048576 count=$size;\n")
        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    // 启动swap
    fun swapOn(priority: Int, useLoop: Boolean = false, keepShell: KeepShell): String {
        val sb = StringBuilder()

        sb.append("sh ")
        sb.append(swapControlScript)
        sb.append(" enable_swap ")
        if (useLoop) {
            sb.append("1")
        } else {
            sb.append("0")
        }
        if (priority > -2) {
            sb.append(" ")
            sb.append(priority)
        }

        return keepShell.doCmdSync(sb.toString())
    }

    // 启动swap
    fun swapOn(priority: Int, useLoop: Boolean = false): String {
        val keepShell = KeepShell()

        val result = swapOn(priority, useLoop, keepShell)

        keepShell.tryExit()
        return result
    }

    // 关闭swap
    fun swapOff() {
        val sb = StringBuilder("sync\necho 3 > /proc/sys/vm/drop_caches\n")

        sb.append("sh ")
        sb.append(swapControlScript)
        sb.append(" disable_swap ")
        if (sceneSwaps.contains("loop")) {
            sb.append("1")
        } else {
            sb.append("0")
        }

        val keepShell = KeepShell()
        val r =keepShell.doCmdSync(sb.toString())
        Log.d("xx", r)
        keepShell.tryExit()
    }

    // 删除swap文件
    fun swapDelete() {
        val sb = StringBuilder("sync\necho 3 > /proc/sys/vm/drop_caches\n")

        sb.append("sh ")
        sb.append(swapControlScript)
        sb.append(" disable_swap ")
        if (sceneSwaps.contains("loop")) {
            sb.append("1")
        } else {
            sb.append("0")
        }

        sb.append("\nrm -f $swapfilePath")

        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    // 是否支持zram
    val zramSupport: Boolean
        get() {
            return KeepShellPublic.doCmdSync(
                    "if [[ ! -e /dev/block/zram0 ]] && [[ -e /sys/class/zram-control ]]; then\n" +
                    "  cat /sys/class/zram-control/hot_add\n" +
                    "fi\n" +
                    "if [[ -e /dev/block/zram0 ]]; then echo 1; else echo 0; fi;") == "1"
        }

    // 是否支持zram WriteBack
    val zramWriteBackSupport: Boolean
        get() {
            return KeepShellPublic.doCmdSync("if [[ -e /sys/block/zram0/backing_dev ]]; then echo 1; else echo 0; fi;") == "1"
        }

    // 是否已启用zram
    val zramEnabled: Boolean
        get() {
            return KeepShellPublic.doCmdSync("cat /proc/swaps | grep /block/zram0").contains("/block/zram0")
        }

    // 关闭zram
    fun zramOff() {
        val sb = StringBuilder("sync\necho 3 > /proc/sys/vm/drop_caches\n")

        sb.append("swapoff /dev/block/zram0\n")
        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    // 调整zram大小
    fun resizeZram(sizeVal: Int, algorithm: String = "") {
        val keepShell = KeepShell()
        val currentSize = zramCurrentSizeMB
        if (currentSize != sizeVal || (algorithm.isNotEmpty() && algorithm != compAlgorithm) || !zramEnabled) {
            val sb = StringBuilder()
            sb.append("echo 4 > /sys/block/zram0/max_comp_streams\n")
            sb.append("sync\n")

            sb.append("if [[ -f /sys/block/zram0/backing_dev ]]; then\n")
            sb.append("  backing_dev=$(cat /sys/block/zram0/backing_dev)\n")
            sb.append("fi\n")

            sb.append("echo 3 > /proc/sys/vm/drop_caches\n")
            sb.append("swapoff /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("echo 1 > /sys/block/zram0/reset\n")

            if (algorithm.isNotEmpty()) {
                sb.append("echo \"$algorithm\" > /sys/block/zram0/comp_algorithm\n")
            }

            sb.append("if [[ -f /sys/block/zram0/backing_dev ]]; then\n")
            sb.append("  echo \"\$backing_dev\" > /sys/block/zram0/backing_dev\n")
            sb.append("fi\n")

            if (sizeVal > 2047) {
                sb.append("echo " + sizeVal + "M > /sys/block/zram0/disksize\n")
            } else {
                sb.append("echo " + (sizeVal * 1024 * 1024L) + " > /sys/block/zram0/disksize\n")
            }

            sb.append("echo 4 > /sys/block/zram0/max_comp_streams\n")
            sb.append("mkswap /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("swapon /dev/block/zram0 -p 0 >/dev/null 2>&1\n")
            keepShell.doCmdSync(sb.toString())
        }

        keepShell.tryExit()
    }

    // ZRAM WritBack 状态
    val writeBackStat: ZramWriteBackStat
        get () {
            return ZramWriteBackStat().apply {
                backingDev = KernelProrp.getProp("/sys/block/zram0/backing_dev")
                val bdStats = KernelProrp.getProp("/sys/block/zram0/bd_stat").trim().split(Regex("[ ]+"))
                if (bdStats.size == 3) {
                    backed = bdStats[0].toInt() * 4
                    backReads = bdStats[1].toInt() * 4
                    backWrites = bdStats[2].toInt() * 4
                }
            }
        }

    val zramCurrentSizeMB: Int
        get () {
            val currentSize = KeepShellPublic.doCmdSync("cat /sys/block/zram0/disksize")
            try {
                return (currentSize.toLong() / 1024 /1024).toInt()
            } catch (ex: java.lang.Exception) {
                return 0
            }
        }

    // 获取可用的ZRAM压缩算法
    val compAlgorithmOptions: Array<String>
        get() {
            val compAlgorithmItems = KernelProrp.getProp("/sys/block/zram0/comp_algorithm").split(" ")
            return compAlgorithmItems.map {
                it.replace("[", "").replace("]", "")
            }.toTypedArray()
        }

    // 获取当前使用的ZRAM压缩算法
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
            KeepShellPublic.doCmdSync("echo 1 > /sys/block/zram0/reset")
            KernelProrp.setProp("/sys/block/zram0/comp_algorithm", value)
        }

    // 强制触发内存回收
    // level    0:极微    1:轻微    2:更重    3:极端
    fun forceKswapd(level: Int): String {
        if (swapForceKswapdScript == null) {
            swapForceKswapdScript = FileWrite.writePrivateShellFile("addin/force_compact.sh", "addin/force_compact.sh", context)
            KeepShellPublic.doCmdSync("rm /cache/force_compact.log 2>/dev/null")
        }

        if (swapForceKswapdScript != null) {
            return KeepShellPublic.getInstance("swap-clear", true).doCmdSync("sh $swapForceKswapdScript $level")
        }
        return "Fail!"
    }

    val procSwaps: MutableList<String>
        get() {
            val ret = KernelProrp.getProp("/proc/swaps")
            var txt = ret.replace("\t\t", "\t").replace("\t", " ")
            while (txt.contains("  ")) {
                txt = txt.replace("  ", " ")
            }
            val rows = txt.split("\n").toMutableList()
            return rows
        }

    val swapUsedSize: Int
        get() {
            var loopName: String? = PropsUtils.getProp("vtools.swap.loop").split("/").lastOrNull()
            if (loopName != null && !loopName.contains("loop")) {
                loopName = null
            }
            for (row in procSwaps) {
                if (row.startsWith("/swapfile ") || row.startsWith("/data/swapfile ") || (loopName != null && row.contains(loopName))) {
                    val cols = row.split(" ").toMutableList()
                    val sizeStr = cols[2]
                    val usedStr = cols[3]

                    try {
                        return usedStr.toInt() / 1024
                    } catch (ex: java.lang.Exception) {
                        break
                    }
                }
            }
            return -1
        }

    val zramUsedSize: Int
        get() {
            for (row in procSwaps) {
                if (row.startsWith("/block/zram0 ") || row.startsWith("/dev/block/zram0 ")) {
                    val cols = row.split(" ").toMutableList()
                    val sizeStr = cols[2]
                    val usedStr = cols[3]

                    try {
                        return usedStr.toInt() / 1024
                    } catch (ex: java.lang.Exception) {
                        break
                    }
                }
            }
            return -1
        }

    val zramPriority: Int?
        get() {
            for (row in procSwaps) {
                if (row.startsWith("/block/zram0 ") || row.startsWith("/dev/block/zram0 ")) {
                    val cols = row.split(" ").toMutableList()

                    try {
                        return cols[4].toInt()
                    } catch (ex: java.lang.Exception) {
                        break
                    }
                }
            }
            return null
        }
}