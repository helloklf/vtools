package com.omarea.vtools.dialogs

import android.app.Activity
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.store.XposedExtension
import com.omarea.vtools.R

class DialogXposedGlobalConfig(var context: Activity) {

    fun show() {
        val xposedExtension = XposedExtension(context)
        xposedExtension.bindService {
            onConnected(xposedExtension)
        }
    }

    private fun onConnected(xposedExtension: XposedExtension) {
        val globalConfig = xposedExtension.getGlobalConfig() ?: return

        val view = context.layoutInflater.inflate(R.layout.dialog_xposed_global_config, null)
        val dialog = DialogHelper.customDialog(context, view)

        val androidScroll = view.findViewById<CompoundButton>(R.id.xposed_android_scroll).apply {
            isChecked = globalConfig.androidScroll
        }
        val hideSuIcon = view.findViewById<CompoundButton>(R.id.xposed_root_icon_hide).apply {
            isChecked = globalConfig.hideSuIcon
        }
        val fgNotificationDisable = view.findViewById<CompoundButton>(R.id.xposed_foreground_disable).apply {
            isChecked = globalConfig.fgNotificationDisable
        }
        val reverseOptimizer = view.findViewById<CompoundButton>(R.id.xposed_reverse_optimizer).apply {
            isChecked = globalConfig.reverseOptimizer
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
            xposedExtension.unbindService()
        }

        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            globalConfig.androidScroll = androidScroll.isChecked
            globalConfig.hideSuIcon = hideSuIcon.isChecked
            globalConfig.fgNotificationDisable = fgNotificationDisable.isChecked
            globalConfig.reverseOptimizer = reverseOptimizer.isChecked

            if (xposedExtension.setGlobalConfig(globalConfig)) {
                xposedExtension.unbindService()
            } else {
                Toast.makeText(context, "保存配置失败！", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
