package com.omarea.vboot.addin

import android.content.Context

/**
 * Created by Hello on 2018/03/22.
 */

class AdbAddin(private var context: Context) : AddinBase(context) {
    fun openNetworkDebug() {
        command = StringBuilder()
                .append("setprop service.adb.tcp.port 5555;")
                .append("stop adbd;")
                .append("sleep 1;")
                .append("start adbd;")
                .toString()

        super.run()
    }
}
