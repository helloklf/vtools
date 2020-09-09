package com.omarea.vtools.dialogs

import android.app.AlertDialog
import android.content.Context
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.vtools.R

/**
 * Created by helloklf on 2017/12/11.
 */

class DialogPower(var context: Context) {
    fun showPowerMenu() {
        DialogHelper.animDialog(
                AlertDialog.Builder(context).setTitle(context.getString(R.string.power_menu)).setItems(
                        arrayOf(context.getString(R.string.power_shutdown),
                                context.getString(R.string.power_reboot),
                                context.getString(R.string.power_hot_reboot),
                                context.getString(R.string.power_fastboot),
                                context.getString(R.string.power_recovery),
                                context.getString(R.string.power_emergency))
                ) { _, w ->
                    when (w) {
                        0 -> KeepShellPublic.doCmdSync(context.getString(R.string.power_shutdown_cmd))
                        1 -> KeepShellPublic.doCmdSync(context.getString(R.string.power_reboot_cmd))
                        2 -> KeepShellPublic.doCmdSync(context.getString(R.string.power_hot_reboot_cmd))
                        3 -> KeepShellPublic.doCmdSync(context.getString(R.string.power_fastboot_cmd))
                        4 -> KeepShellPublic.doCmdSync(context.getString(R.string.power_recovery_cmd))
                        5 -> KeepShellPublic.doCmdSync(context.getString(R.string.power_emergency_cmd))
                    }
                })
    }
}
