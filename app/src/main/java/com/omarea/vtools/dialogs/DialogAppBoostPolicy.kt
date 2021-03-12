package com.omarea.vtools.dialogs

import android.app.Activity
import android.view.View
import android.widget.CompoundButton
import com.omarea.common.ui.DialogHelper
import com.omarea.vtools.R

class DialogAppBoostPolicy(var context: Activity, val current: Boolean, val iResultCallback: IResultCallback) {
    interface IResultCallback {
        fun onChange(enabled: Boolean)
    }

    fun show() {
        val view = context.layoutInflater.inflate(R.layout.dialog_scene_app_boost, null)
        val dialog = DialogHelper.customDialog(context, view)
        val switch = view.findViewById<CompoundButton>(R.id.boost_policy_mem)
        switch.isChecked = current

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            iResultCallback.onChange(switch.isChecked)
        }
    }
}
