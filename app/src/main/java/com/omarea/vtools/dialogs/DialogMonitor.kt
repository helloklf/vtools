package com.omarea.vtools.dialogs

import android.app.Activity
import android.view.View
import android.widget.CompoundButton
import com.omarea.common.ui.DialogHelper
import com.omarea.library.shell.ProcessUtils
import com.omarea.vtools.R
import com.omarea.vtools.popup.FloatMonitor
import com.omarea.vtools.popup.FloatMonitorGame
import com.omarea.vtools.popup.FloatTaskManager

class DialogMonitor(var context: Activity) {
    fun show() {
        val view = context.layoutInflater.inflate(R.layout.dialog_float_monitor, null)
        val dialog = DialogHelper.customDialogBlurBg(context, view)
        val perf = view.findViewById<CompoundButton>(R.id.monitor_perf)
        val proc = view.findViewById<CompoundButton>(R.id.monitor_proc)
        val game = view.findViewById<CompoundButton>(R.id.monitor_game)

        perf.isChecked = FloatMonitor.show == true
        proc.isChecked = FloatTaskManager.show == true
        proc.isEnabled = ProcessUtils().supported(context)
        game.isChecked = FloatMonitorGame.show == true

        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            if (perf.isChecked) {
                FloatMonitor(context).showPopupWindow()
            } else {
                FloatMonitor(context).hidePopupWindow()
            }
            if (proc.isChecked) {
                FloatTaskManager(context).showPopupWindow()
            } else {
                FloatTaskManager(context).hidePopupWindow()
            }
            if (game.isChecked) {
                FloatMonitorGame(context).showPopupWindow()
            } else {
                FloatMonitorGame(context).hidePopupWindow()
            }
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
    }
}
