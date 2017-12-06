package com.omarea.vboot.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shell.SuDo
import com.omarea.vboot.R

/**
 * Created by Hello on 2017/12/03.
 */

class dialog_addin_modifydevice {
    var context: Context

    constructor(context: Context) {
        this.context = context
    }

    fun modifyDeviceInfo() {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_addin_device, null)
        editModel = dialog.findViewById(R.id.dialog_addin_model) as EditText
        editBrand = dialog.findViewById(R.id.dialog_addin_brand) as EditText
        editProductName = dialog.findViewById(R.id.dialog_addin_name) as EditText
        editDevice = dialog.findViewById(R.id.dialog_addin_device) as EditText
        editManufacturer = dialog.findViewById(R.id.dialog_addin_manufacturer) as EditText

        (dialog.findViewById(R.id.dialog_addin_default) as Button).setOnClickListener { v: View? ->
            setDefault()
        }
        (dialog.findViewById(R.id.dialog_addin_x20) as Button).setOnClickListener { v: View? ->
            setX20()
        }
        (dialog.findViewById(R.id.dialog_addin_r11) as Button).setOnClickListener { v: View? ->
            setR11Plus()
        }
        (dialog.findViewById(R.id.dialog_addin_ipx) as Button).setOnClickListener { v: View? ->
            setIPhoneX()
        }
        AlertDialog.Builder(context).setTitle("机型信息修改").setView(dialog).setNegativeButton("保存重启", { d, w ->
            val sb = StringBuilder()
            val model = editModel.text.trim()
            val brand = editBrand.text.trim()
            val product = editProductName.text.trim()
            val device = editDevice.text.trim()
            val manufacturer = editManufacturer.text.trim()
            if (model.length > 0 || brand.length > 0 || product.length > 0 || device.length > 0 || manufacturer.length > 0) {
                backupDefault()
                sb.append(Consts.MountSystemRW)
                sb.append("cp /system/build.prop /data/build.prop;chmod 0644 /data/build.prop;")

                if (brand.length > 0)
                    sb.append("busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=$brand/' /data/build.prop;")
                if (product.length > 0)
                    sb.append("busybox sed -i 's/^ro.product.name=.*/ro.product.name=$product/' /data/build.prop;")
                if (model.length > 0)
                    sb.append("busybox sed -i 's/^ro.product.model=.*/ro.product.model=$model/' /data/build.prop;")
                if (manufacturer.length > 0)
                    sb.append("busybox sed -i 's/^ro.product.manufacturer=.*/ro.product.manufacturer=$manufacturer/' /data/build.prop;")
                if (device.length > 0)
                    sb.append("busybox sed -i 's/^ro.product.device=.*/ro.product.device=$device/' /data/build.prop;")

                sb.append("cp /system/build.prop /system/build.bak.prop\n")
                sb.append("cp /data/build.prop /system/build.prop\n")
                sb.append("rm /data/build.prop\n")
                sb.append("chmod 0644 /system/build.prop\n")
                sb.append("sync\n")
                sb.append("reboot\n")

                SuDo(context).execCmdSync(sb.toString())
            } else {
                Toast.makeText(context, "什么也没有修改！", Toast.LENGTH_SHORT).show()
            }
        }).create().show()
    }

    lateinit var editModel: EditText
    lateinit var editBrand: EditText
    lateinit var editProductName: EditText
    lateinit var editDevice: EditText
    lateinit var editManufacturer: EditText

    private fun setDefault() {
        val spf = context.getSharedPreferences("deviceinfo", Context.MODE_PRIVATE)
        if (spf.all.size == 0) {
            editBrand.setText(android.os.Build.BRAND)
            editModel.setText(android.os.Build.MODEL)
            editProductName.setText(android.os.Build.PRODUCT)
            editDevice.setText(android.os.Build.DEVICE)
            editManufacturer.setText(android.os.Build.MANUFACTURER)
        } else {
            editBrand.setText(spf.getString("android.os.Build.BRAND", android.os.Build.BRAND))
            editModel.setText(spf.getString("android.os.Build.MODEL", android.os.Build.MODEL))
            editProductName.setText(spf.getString("android.os.Build.PRODUCT", android.os.Build.PRODUCT))
            editDevice.setText(spf.getString("android.os.Build.DEVICE", android.os.Build.DEVICE))
            editManufacturer.setText(spf.getString("android.os.Build.MANUFACTURER", android.os.Build.MANUFACTURER))
        }
    }

    private fun setX20() {
        editBrand.setText("vivo")
        editModel.setText("vivo X20")
        editProductName.setText("X20")
        editDevice.setText("X20")
        editManufacturer.setText("vivo")
    }

    private fun setR11Plus() {
        editBrand.setText("OPPO")
        editModel.setText("OPPO R11 Plus")
        editProductName.setText("R11 Plus")
        editDevice.setText("R11 Plus")
        editManufacturer.setText("OPPO")
    }

    private fun setIPhoneX() {
        editBrand.setText("iPhone")
        editModel.setText("X")
        editProductName.setText("hydrogen")
        editDevice.setText("hydrogen")
        editManufacturer.setText("iPhone")
    }

    private fun backupDefault() {
        val spf = context.getSharedPreferences("deviceinfo", Context.MODE_PRIVATE)
        if (spf.all.size == 0) {
            spf.edit()
                    .putString("android.os.Build.BRAND", android.os.Build.BRAND)
                    .putString("android.os.Build.MODEL", android.os.Build.MODEL)
                    .putString("android.os.Build.PRODUCT", android.os.Build.PRODUCT)
                    .putString("android.os.Build.DEVICE", android.os.Build.DEVICE)
                    .putString("android.os.Build.MANUFACTURER", android.os.Build.MANUFACTURER)
                    .commit()
        }
    }
}
