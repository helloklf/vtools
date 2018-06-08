package com.omarea.vboot.dialogs

import android.app.AlertDialog
import android.content.Context
import com.omarea.shell.SuDo

/**
 * Created by helloklf on 2017/12/11.
 */

class DialogPower(var context: Context) {
    fun showPowerMenu() {
        AlertDialog.Builder(context).setTitle("请选择操作").setItems(arrayOf("快速关机", "快速重启", "热重启", "进入Fastboot", "进入Recovery", "进入9008模式"), { _, w ->
            val sudo = SuDo(context)
            when (w) {
                0 -> sudo.execCmdSync("sync;reboot -p;")
                1 -> sudo.execCmdSync("sync;reboot;")
                2 -> sudo.execCmdSync("sync;busybox killall system_server;")
                3 -> sudo.execCmdSync("sync;reboot bootloader;")
                4 -> sudo.execCmdSync("sync;reboot recovery;")
                5 -> sudo.execCmdSync("sync;reboot edl;")
            }
        }).create().show()
    }
}
