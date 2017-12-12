package com.omarea.shell.units

import android.content.Context
import com.omarea.shell.SuDo

/**
 * Created by Hello on 2017/11/01.
 */

class ChangeZRAM(private var context: Context, private var swapfilePath:String = "/data/swapfile") {
    /*
        1 -> {
            stringBuilder.append(
                    "swapoff /dev/block/zram0\n" +
                            "echo 1 > /sys/block/zram0/reset\n" +
                            "echo 597000000 > /sys/block/zram0/disksize\n" +
                            "mkswap /dev/block/zram0 &> /dev/null\n" +
                            "swapon /dev/block/zram0 &> /dev/null\n" +
                            "echo 100 > /proc/sys/vm/swappiness\n")
        }*/
    fun createSwapFile(size: String) {
        val sb = StringBuilder()
        sb.append("swapoff $swapfilePath >/dev/null 2>&1;\n")
        sb.append("dd if=/dev/zero of=$swapfilePath bs=1048576 count=$size;\n")
        sb.append("mkswap $swapfilePath;\n")
        SuDo(context).execCmdSync(sb.toString())
    }
}