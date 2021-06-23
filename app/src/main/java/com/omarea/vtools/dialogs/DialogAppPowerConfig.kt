package com.omarea.vtools.dialogs

import android.app.Activity
import android.view.View
import com.omarea.common.ui.DialogHelper
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.vtools.R

class DialogAppPowerConfig(var context: Activity, val current: String?, val iResultCallback: IResultCallback) {
    interface IResultCallback {
        fun toMode() {}
        fun onChange(mode: String?)
    }

    private fun onModeClick (mode: String?) {
        if (mode != current) {
            iResultCallback.onChange(mode)
        }
    }

    fun show() {
        val view = context.layoutInflater.inflate(R.layout.dialog_scene_app_powercfg, null)
        val dialog = DialogHelper.customDialog(context, view)

        view.findViewById<View>(R.id.mode_powersave).run {
            setOnClickListener {
                dialog.dismiss()
                onModeClick(ModeSwitcher.POWERSAVE)
            }
            alpha = if (current == ModeSwitcher.POWERSAVE) 1f else 0.5f
        }
        view.findViewById<View>(R.id.mode_balance).run {
            setOnClickListener {
                dialog.dismiss()
                onModeClick(ModeSwitcher.BALANCE)
            }
            alpha = if (current == ModeSwitcher.BALANCE) 1f else 0.5f
        }
        view.findViewById<View>(R.id.mode_performance).run {
            setOnClickListener {
                dialog.dismiss()
                onModeClick(ModeSwitcher.PERFORMANCE)
            }
            alpha = if (current == ModeSwitcher.PERFORMANCE) 1f else 0.5f
        }
        view.findViewById<View>(R.id.mode_fast).run {
            setOnClickListener {
                dialog.dismiss()
                onModeClick(ModeSwitcher.FAST)
            }
            alpha = if (current == ModeSwitcher.FAST) 1f else 0.5f
        }

        view.findViewById<View>(R.id.mode_empty).run {
            setOnClickListener {
                dialog.dismiss()
                onModeClick("")
            }
            alpha = if (current.isNullOrEmpty()) 1f else 0.5f
        }
        view.findViewById<View>(R.id.mode_keep).run {
            setOnClickListener {
                dialog.dismiss()
                onModeClick(ModeSwitcher.IGONED)
            }
            alpha = if (current == ModeSwitcher.IGONED) 1f else 0.5f
        }

        view.findViewById<View>(R.id.action_more).setOnClickListener {
            dialog.dismiss()
            iResultCallback.toMode()
        }
    }
}
