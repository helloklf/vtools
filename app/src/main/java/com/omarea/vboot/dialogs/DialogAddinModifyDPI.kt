package com.omarea.vboot.dialogs

import android.content.Context
import android.os.Build
import android.support.v7.app.AlertDialog
import android.util.DisplayMetrics
import android.view.Display
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import com.omarea.shared.Consts
import com.omarea.shell.SuDo
import com.omarea.vboot.R

/**
 * Created by Hello on 2017/12/03.
 */

class DialogAddinModifyDPI(var context: Context) {

    fun modifyDPI(display: Display) {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_addin_dpi, null)
        val dpiInput = dialog.findViewById(R.id.dialog_addin_dpi_dpiinput) as EditText
        val widthInput = dialog.findViewById(R.id.dialog_addin_dpi_width) as EditText
        val heightInput = dialog.findViewById(R.id.dialog_addin_dpi_height) as EditText
        val quickChange = dialog.findViewById(R.id.dialog_addin_dpi_quickchange) as CheckBox

        val dm = DisplayMetrics()
        display.getMetrics(dm)
        dpiInput.setText(dm.densityDpi.toString())
        widthInput.setText(dm.widthPixels.toString())
        heightInput.setText(dm.heightPixels.toString())
        if (Build.VERSION.SDK_INT >= 24) {
            quickChange.isChecked = true
        }

        val rate = dm.heightPixels / 1.0 / dm.widthPixels
        dialog.findViewById<Button>(R.id.dialog_dpi_720).setOnClickListener({
            widthInput.setText("720")
            if (rate > 1.8) {
                heightInput.setText("1440")
            } else {
                heightInput.setText("1280")
            }
            dpiInput.setText("320")
        })
        dialog.findViewById<Button>(R.id.dialog_dpi_1080).setOnClickListener({
            widthInput.setText("1080")
            if (rate > 1.8) {
                heightInput.setText("2160")
            } else {
                heightInput.setText("1920")
            }
            dpiInput.setText("480")
        })
        dialog.findViewById<Button>(R.id.dialog_dpi_2k).setOnClickListener({
            widthInput.setText("1440")
            if (rate > 1.8) {
                heightInput.setText("2960")
            } else {
                heightInput.setText("2560")
            }
            dpiInput.setText("640")
        })
        dialog.findViewById<Button>(R.id.dialog_dpi_4k).setOnClickListener({
            widthInput.setText("2160")
            if (rate > 1.8) {
                heightInput.setText("4440")
            } else {
                heightInput.setText("3840")
            }
            dpiInput.setText("960")
        })

        AlertDialog.Builder(context).setTitle("DPI、分辨率").setView(dialog).setNegativeButton("确定", { _, _ ->
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
                cmd.append("wm density $dpi")
                cmd.append("\n")
            }
            if (!qc && dpi >= 96) {
                cmd.append(Consts.MountSystemRW)
                cmd.append("wm density reset\n")
                cmd.append("sed '/ro.sf.lcd_density=/'d /system/build.prop > /data/build.prop\n")
                cmd.append("sed '\$aro.sf.lcd_density=$dpi' /data/build.prop > /data/build2.prop\n")
                cmd.append("cp /system/build.prop /system/build.prop.dpi_bak\n")
                cmd.append("cp /data/build2.prop /system/build.prop\n")
                cmd.append("rm /data/build.prop\n")
                cmd.append("rm /data/build2.prop\n")
                cmd.append("chmod 0644 /system/build.prop\n")
                cmd.append("sync\n")
                cmd.append("reboot\n")
            }
            if (cmd.isNotEmpty())
                SuDo(context).execCmdSync(cmd.toString())
        }).create().show()
    }
}
