package com.omarea.vtools.dialogs

import android.app.AlertDialog
import android.content.Context
import com.omarea.shell.KeepShellSync

/**
 * Created by helloklf on 2017/12/11.
 */

class DialogPower(var context: Context) {
    fun showPowerMenu() {
        AlertDialog.Builder(context).setTitle("请选择操作").setItems(arrayOf("快速关机", "快速重启", "热重启", "进入Fastboot", "进入Recovery", "进入9008模式"), { _, w ->
            when (w) {
                0 -> KeepShellSync.doCmdSync("sync;reboot -p;")
                1 -> KeepShellSync.doCmdSync("sync;reboot;")
                2 -> KeepShellSync.doCmdSync("sync;busybox killall system_server;")
                3 -> KeepShellSync.doCmdSync("sync;reboot bootloader;")
                4 -> KeepShellSync.doCmdSync("sync;reboot recovery;")
                5 -> KeepShellSync.doCmdSync("sync;reboot edl;")
            }
        }).create().show()
    }
}
