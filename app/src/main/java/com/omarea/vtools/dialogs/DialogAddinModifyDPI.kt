package com.omarea.vtools.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.LayoutInflater
import android.widget.*
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import java.util.*

/**
 * Created by Hello on 2017/12/03.
 */

class DialogAddinModifyDPI(var context: Activity) {
    private val BACKUP_SCREEN_DPI: String = "screen_dpi"
    private val BACKUP_SCREEN_RATIO: String = "screen_ratio"
    private val BACKUP_SCREEN_WIDTH: String = "screen_width"
    private val DEFAULT_RATIO: Float = 16 / 9f
    private val DEFAULT_DPI: Int = 320
    private val DEFAULT_WIDTH: Int = 720

    @SuppressLint("ApplySharedPref")
    private fun backupDisplay(point: Point, dm: DisplayMetrics, context: Context) {
        val spf = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE);
        if (!spf.contains(BACKUP_SCREEN_RATIO)) {
            spf.edit().putFloat(BACKUP_SCREEN_RATIO, point.y / point.x.toFloat()).commit()
        }
        if (!spf.contains(BACKUP_SCREEN_DPI) || !spf.contains(BACKUP_SCREEN_WIDTH)) {
            spf.edit().putInt(BACKUP_SCREEN_DPI, dm.densityDpi).commit()
            spf.edit().putInt(BACKUP_SCREEN_WIDTH, point.x).commit()
        }
    }

    private fun getHeightScaleValue(width: Int): Int {
        val spf = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE);
        return (width * spf.getFloat(BACKUP_SCREEN_RATIO, DEFAULT_RATIO)).toInt()
    }

    private fun getDpiScaleValue(width: Int): Int {
        val spf = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE);
        return (spf.getInt(BACKUP_SCREEN_DPI, DEFAULT_DPI) * width / spf.getInt(BACKUP_SCREEN_WIDTH, DEFAULT_WIDTH))
    }

    fun modifyDPI(display: Display, context: Activity) {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_addin_dpi, null)
        val dpiInput = dialog.findViewById(R.id.dialog_addin_dpi_dpiinput) as EditText
        val widthInput = dialog.findViewById(R.id.dialog_addin_dpi_width) as EditText
        val heightInput = dialog.findViewById(R.id.dialog_addin_dpi_height) as EditText
        val quickChange = dialog.findViewById(R.id.dialog_addin_dpi_quickchange) as CheckBox

        val dm = DisplayMetrics()
        display.getMetrics(dm)
        val point = Point()
        display.getRealSize(point)

        backupDisplay(point, dm, context);

        dpiInput.setText(dm.densityDpi.toString())
        widthInput.setText(point.x.toString())
        heightInput.setText(point.y.toString())

        if (Build.VERSION.SDK_INT >= 24) {
            quickChange.isChecked = true
        }

        val rate = dm.heightPixels / 1.0 / dm.widthPixels
        dialog.findViewById<Button>(R.id.dialog_dpi_720).setOnClickListener {
            val width = 720
            widthInput.setText(width.toString())
            val height = getHeightScaleValue(width)
            heightInput.setText(height.toString())
            dpiInput.setText((dm.densityDpi.toFloat() * width / point.x).toInt().toString())
        }
        dialog.findViewById<Button>(R.id.dialog_dpi_1080).setOnClickListener {
            val width = 1080
            widthInput.setText(width.toString())
            heightInput.setText(getHeightScaleValue(width).toString())
            dpiInput.setText(getDpiScaleValue(width).toString())
        }
        dialog.findViewById<Button>(R.id.dialog_dpi_2k).setOnClickListener {
            val width = 1440
            widthInput.setText(width.toString())
            heightInput.setText(getHeightScaleValue(width).toString())
            dpiInput.setText(getDpiScaleValue(width).toString())
        }
        dialog.findViewById<Button>(R.id.dialog_dpi_4k).setOnClickListener {
            val width = 2160
            widthInput.setText(width.toString())
            heightInput.setText(getHeightScaleValue(width).toString())
            dpiInput.setText(getDpiScaleValue(width).toString())
        }

         val dialogInstance = DialogHelper.confirm(context, "DPI、分辨率", "", dialog, {
            val dpi = if (dpiInput.text.isNotEmpty()) (dpiInput.text.toString().toInt()) else (0)
            val width = if (widthInput.text.isNotEmpty()) (widthInput.text.toString().toInt()) else (0)
            val height = if (heightInput.text.isNotEmpty()) (heightInput.text.toString().toInt()) else (0)
            val qc = quickChange.isChecked

            val cmd = StringBuilder()
            if (width >= 320 && height >= 480) {
                cmd.append("wm size ${width}x$height")
                cmd.append("\n")
            }
            if (dpi >= 96) {
                if (qc) {
                    cmd.append("wm density $dpi")
                    cmd.append("\n")
                } else {
                    if (MagiskExtend.moduleInstalled()) {
                        KeepShellPublic.doCmdSync("wm density reset");
                        MagiskExtend.setSystemProp("ro.sf.lcd_density", dpi.toString());
                        MagiskExtend.setSystemProp("vendor.display.lcd_density", dpi.toString());
                        Toast.makeText(context, "已通过Magisk更改参数，请重启手机~", Toast.LENGTH_SHORT).show()
                    } else {
                        cmd.append(CommonCmds.MountSystemRW)
                        cmd.append("wm density reset\n")
                        cmd.append("sed '/ro.sf.lcd_density=/'d /system/build.prop > /data/build.prop\n")
                        cmd.append("sed '\$aro.sf.lcd_density=$dpi' /data/build.prop > /data/build2.prop\n")
                        cmd.append("cp /system/build.prop /system/build.prop.dpi_bak\n")
                        cmd.append("cp /data/build2.prop /system/build.prop\n")
                        cmd.append("rm /data/build.prop\n")
                        cmd.append("rm /data/build2.prop\n")
                        cmd.append("chmod 0755 /system/build.prop\n")
                        cmd.append("sync\n")
                        cmd.append("reboot\n")
                    }
                }
            }
            if (cmd.isNotEmpty())
                KeepShellPublic.doCmdSync(cmd.toString())

            if (qc) {
                autoResetConfirm()
            }
        }, {

        })

        dialog.findViewById<Button>(R.id.dialog_dpi_reset).setOnClickListener {
            if (dialogInstance.isShowing == true) {
                try {
                    dialogInstance.dismiss()
                } catch (ex: java.lang.Exception) {
                }
            }
            resetDisplay()
        }
    }

    private fun autoResetConfirm() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_addin_dpi_confirm, null)
            val timeoutView = view.findViewById<TextView>(R.id.dpi_modify_timeout)
            val dialog = DialogHelper.customDialog(context, view, false)

            var timeOut = 30
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    timeOut -= 1
                    if (timeOut < 1) {
                        cancel()
                        if (dialog.isShowing) {
                            handler.post {
                                try {
                                    dialog.dismiss()
                                } catch (ex: Exception) {
                                }
                            }
                            resetDisplay()
                            pointerLocationOff()
                        }
                    } else {
                        handler.post {
                            try {
                                timeoutView.setText(timeOut.toString())
                            } catch (ex: Exception) {
                            }
                        }
                    }
                }
            }, 1000, 1000)

            timeoutView.setText(timeOut.toString())
            view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
                resetDisplay()
                pointerLocationOff()
            }
            view.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                dialog.dismiss()
                pointerLocationOff()
            }
            KeepShellPublic.doCmdSync("settings put system pointer_location 1")
        }, 2000)
    }

    private fun pointerLocationOff() {
        KeepShellPublic.doCmdSync("settings put system pointer_location 0")
    }

    private fun resetDisplay() {
        val cmd = StringBuilder()
        cmd.append("wm size reset\n")
        cmd.append("wm density reset\n")
        cmd.append("wm overscan reset\n")
        KeepShellPublic.doCmdSync(cmd.toString())
    }
}
