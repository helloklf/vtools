package com.omarea.vtools.dialogs

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.View
import android.widget.CompoundButton
import com.omarea.common.ui.DialogHelper
import com.omarea.library.basic.RadioGroupSimulator
import com.omarea.vtools.R

class DialogAppOrientation(var context: Activity, val current: Int?, val iResultCallback: IResultCallback) {
    interface IResultCallback {
        fun onChange(value: Int, name: String?)
    }

    class Transform(private val context: Context) {
        private val res = context.resources
        private val groupNames = ArrayList<String>().apply {
        }

        fun getName(value: Int?): String {
            return when (value) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> "默认"
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR -> "强制旋转"
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER -> "自动旋转"
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> "强制横屏"
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT -> "强制竖屏"
                else -> "未知设置"
            }
        }
    }

    fun getName(value: Int?): String {
        return Transform(context).getName(value)
    }

    fun show() {
        val view = context.layoutInflater.inflate(R.layout.dialog_scene_app_orientation, null)
        val dialog = DialogHelper.customDialog(context, view)
        val group = RadioGroupSimulator(
                view.findViewById<CompoundButton>(R.id.orientation_default).apply {
                    tag = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                },
                view.findViewById<CompoundButton>(R.id.orientation_sensor_force).apply {
                    tag = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                },
                view.findViewById<CompoundButton>(R.id.orientation_sensor_auto).apply {
                    tag = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                },
                view.findViewById<CompoundButton>(R.id.orientation_landscape).apply {
                    tag = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                },
                view.findViewById<CompoundButton>(R.id.orientation_portrait).apply {
                    tag = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
        )
        if (current != null) {
            group.setCheckedByTag(current)
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            val result = group.checked?.tag as Int?
            if (result != null) {
                iResultCallback.onChange(result, getName(result))
            } else {
                iResultCallback.onChange(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, getName(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED))
            }
        }
    }
}
