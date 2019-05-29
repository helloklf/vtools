package com.omarea.shell.units

import android.content.Context
import com.omarea.common.shell.KeepShellPublic

/**
 * Created by Hello on 2017/11/01.
 */

class ChangeZRAM(private var context: Context, private var swapfilePath: String = "/data/swapfile") {
    fun createSwapFile(size: Int) {
        val sb = StringBuilder()
        sb.append("swapoff $swapfilePath >/dev/null 2>&1;\n")
        sb.append("dd if=/dev/zero of=$swapfilePath bs=1048576 count=$size;\n")
        sb.append("mkswap $swapfilePath;\n")
        KeepShellPublic.doCmdSync(sb.toString())
    }
}