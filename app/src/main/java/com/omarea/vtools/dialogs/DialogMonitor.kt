package com.omarea.vtools.dialogs

import android.app.Activity
import android.view.View
import android.widget.CompoundButton
import com.omarea.common.ui.DialogHelper
import com.omarea.vtools.R
import com.omarea.vtools.popup.FloatMonitor
import com.omarea.vtools.popup.FloatMonitorGame

class DialogMonitor(var context: Activity) {
    fun show() {
        val view = context.layoutInflater.inflate(R.layout.dialog_float_monitor, null)
        val dialog = DialogHelper.customDialog(context, view)

        view.findViewById<CompoundButton>(R.id.monitor_perf).run {
            isChecked = FloatMonitor.show == true
            setOnClickListener {
                if (isChecked) {
                    FloatMonitor(context).showPopupWindow()
                } else {
                    FloatMonitor(context).hidePopupWindow()
                }
            }
        }
        view.findViewById<CompoundButton>(R.id.monitor_game).run {
            isChecked = FloatMonitorGame.show == true
            setOnClickListener {
                if (isChecked) {
                    FloatMonitorGame(context).showPopupWindow()
                } else {
                    FloatMonitorGame(context).hidePopupWindow()
                }
            }
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
    }
}
