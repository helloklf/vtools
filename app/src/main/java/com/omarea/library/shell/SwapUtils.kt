package com.omarea.library.shell

import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile
import java.io.File

/**
 * Created by Hello on 2017/11/01.
 */

class SwapUtils(private val context: Context) {
    private var swapfilePath: String = "/data/swapfile"
    private var swapForceKswapdScript:String? = null
    // private var zramControlScript = FileWrite.writePrivateShellFile("addin/zram_control.sh", "addin/zram_control.sh", context)

    // 是否已创建swapfile文件
    val swapExists: Boolean
        get() {
            return RootFile.itemExists(swapfilePath)
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

    val zramCurrentSizeMB: Int
        get () {
            val currentSize = KeepShellPublic.doCmdSync("cat /sys/block/zram0/disksize")
            try {
                return (currentSize.toLong() / 1024 /1024).toInt()
            } catch (ex: java.lang.Exception) {
                return 0
            }
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
            for (row in procSwaps) {
                if (row.startsWith("/swapfile ") || row.startsWith("/data/swapfile ")) {
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