package com.omarea.vtools.dialogs

import android.app.AlertDialog
import android.content.Context
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper

/**
 * Created by helloklf on 2017/12/11.
 */

class DialogPower(var context: Context) {
    fun showPowerMenu() {
        DialogHelper.animDialog(
                AlertDialog.Builder(context).setTitle("请选择操作").setItems(
                        arrayOf("快速关机", "快速重启", "热重启", "进入Fastboot", "进入Recovery", "进入9008模式"),
                        { _, w ->
                            when (w) {
                                0 -> KeepShellPublic.doCmdSync("sync;reboot -p;")
                                1 -> KeepShellPublic.doCmdSync("sync;reboot;")
                                2 -> KeepShellPublic.doCmdSync("sync;busybox killall system_server;")
                                3 -> KeepShellPublic.doCmdSync("sync;reboot bootloader;")
                                4 -> KeepShellPublic.doCmdSync("sync;reboot recovery;")
                                5 -> KeepShellPublic.doCmdSync("sync;reboot edl;")
                            }
                        }))
    }
}
