package com.omarea.vtools.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.util.Base64
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.PropsUtils
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R

/**
 * Created by Hello on 2017/12/03.
 */

class DialogAddinModifydevice(var context: Context) {

    val BACKUP_SUCCESS = "persist.vtools.device.backuped"
    val BACKUP_BRAND = "persist.vtools.brand"
    val BACKUP_MODEL = "persist.vtools.model"
    val BACKUP_PRODUCT = "persist.vtools.product"
    val BACKUP_DEVICE = "persist.vtools.device"
    val BACKUP_MANUFACTURER = "persist.vtools.manufacturer"

    private fun getBackupProp(prop: String, default: String): String {
        val value = PropsUtils.getProp(prop)
        if (value == "null" || value == "") {
            return default
        }

        return value
    }

    fun modifyDeviceInfo() {
        //SM-N9500@samsung@samsung@dream2qltezc@dream2qltechn

        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_addin_device, null)
        editModel = dialog.findViewById(R.id.dialog_addin_model) as EditText
        editBrand = dialog.findViewById(R.id.dialog_addin_brand) as EditText
        editProductName = dialog.findViewById(R.id.dialog_addin_name) as EditText
        editDevice = dialog.findViewById(R.id.dialog_addin_device) as EditText
        editManufacturer = dialog.findViewById(R.id.dialog_addin_manufacturer) as EditText

        (dialog.findViewById(R.id.dialog_addin_default) as Button).setOnClickListener {
            setDefault()
        }
        (dialog.findViewById(R.id.dialog_chooser) as Button).setOnClickListener {
            templateChooser()
        }
        DialogHelper.animDialog(AlertDialog.Builder(context)
                //.setTitle("机型信息修改")
                .setView(dialog).setNegativeButton("保存重启", { _, _ ->
                    val model = editModel.text.trim()
                    val brand = editBrand.text.trim()
                    val product = editProductName.text.trim()
                    val device = editDevice.text.trim()
                    val manufacturer = editManufacturer.text.trim()
                    if (model.isNotEmpty() || brand.isNotEmpty() || product.isNotEmpty() || device.isNotEmpty() || manufacturer.isNotEmpty()) {
                        backupDefault()
                        if (com.omarea.common.shared.MagiskExtend.moduleInstalled()) {
                            if (brand.isNotEmpty())
                                com.omarea.common.shared.MagiskExtend.setSystemProp("ro.product.brand", brand.toString())
                            if (product.isNotEmpty())
                                com.omarea.common.shared.MagiskExtend.setSystemProp("ro.product.name", product.toString())
                            if (model.isNotEmpty())
                                com.omarea.common.shared.MagiskExtend.setSystemProp("ro.product.model", model.toString())
                            if (manufacturer.isNotEmpty())
                                com.omarea.common.shared.MagiskExtend.setSystemProp("ro.product.manufacturer", manufacturer.toString())
                            if (device.isNotEmpty())
                                com.omarea.common.shared.MagiskExtend.setSystemProp("ro.product.device", device.toString())
                            // 小米 - 改model参数以后device_features要处理下
                            if (RootFile.fileExists("/system/etc/device_features/${android.os.Build.PRODUCT}.xml")) {
                                if (model != android.os.Build.PRODUCT) {
                                    com.omarea.common.shared.MagiskExtend.replaceSystemFile("/system/etc/device_features/${product}.xml", "/system/etc/device_features/${android.os.Build.PRODUCT}.xml")
                                }
                            }
                            Toast.makeText(context, "已通过Magisk更改参数，请重启手机~", Toast.LENGTH_SHORT).show()
                        } else {
                            val sb = StringBuilder()
                            sb.append(CommonCmds.MountSystemRW)
                            sb.append("cp /system/build.prop /data/build.prop;chmod 0755 /data/build.prop;")

                            if (brand.isNotEmpty())
                                sb.append("busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=$brand/' /data/build.prop;")
                            if (product.isNotEmpty())
                                sb.append("busybox sed -i 's/^ro.product.name=.*/ro.product.name=$product/' /data/build.prop;")
                            if (model.isNotEmpty())
                                sb.append("busybox sed -i 's/^ro.product.model=.*/ro.product.model=$model/' /data/build.prop;")
                            if (manufacturer.isNotEmpty())
                                sb.append("busybox sed -i 's/^ro.product.manufacturer=.*/ro.product.manufacturer=$manufacturer/' /data/build.prop;")
                            if (device.isNotEmpty())
                                sb.append("busybox sed -i 's/^ro.product.device=.*/ro.product.device=$device/' /data/build.prop;")

                            sb.append("cp /system/build.prop /system/build.bak.prop\n")
                            sb.append("cp /data/build.prop /system/build.prop\n")
                            sb.append("rm /data/build.prop\n")
                            sb.append("chmod 0755 /system/build.prop\n")

                            // 小米 - 改model参数以后device_features要处理下
                            if (RootFile.fileExists("/system/etc/device_features/${android.os.Build.PRODUCT}.xml")) {
                                if (model != android.os.Build.PRODUCT) {
                                    KeepShellPublic.doCmdSync("cp \"/system/etc/device_features/${android.os.Build.PRODUCT}.xml\" \"/system/etc/device_features/${product}.xml\"")
                                }
                            }

                            sb.append("sync\n")
                            sb.append("reboot\n")

                            KeepShellPublic.doCmdSync(sb.toString())
                        }
                    } else {
                        Toast.makeText(context, "什么也没有修改！", Toast.LENGTH_SHORT).show()
                    }
                }).setPositiveButton("使用帮助", { _, _ ->
                    DialogHelper.animDialog(AlertDialog.Builder(context).setMessage(R.string.dialog_addin_device_desc).setNegativeButton(R.string.btn_confirm, { _, _ -> }))
                }))
        loadCurrent()

        try {
            val cm = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val data = cm.getPrimaryClip()
            val item = data.getItemAt(0)
            val content = item.getText()
            if (content.isNotEmpty()) {
                val copyData = String(Base64.decode(content.toString().trim(), Base64.DEFAULT))
                if (Regex("^.*@.*@.*@.*@.*\$").matches(copyData)) {
                    DialogHelper.animDialog(AlertDialog.Builder(context)
                            .setTitle("可用的模板")
                            .setMessage("检测到已复制的机型信息：\n\n" + copyData + "\n\n是否立即使用？")
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                splitCodeStr(copyData)
                            })
                            .setNegativeButton(R.string.btn_cancel, null))
                }
            }
        } catch (ex: Exception) {
        }
    }

    private fun splitCodeStr(codeStr: String) {
        if (Regex("^.*@.*@.*@.*@.*\$").matches(codeStr)) {
            val deviceInfos = codeStr.split("@")
            editModel.setText(deviceInfos[0])
            editBrand.setText(deviceInfos[1])
            editManufacturer.setText(deviceInfos[2])
            editProductName.setText(deviceInfos[3])
            editDevice.setText(deviceInfos[4])
        }
    }

    private lateinit var editModel: EditText
    private lateinit var editBrand: EditText
    private lateinit var editProductName: EditText
    private lateinit var editDevice: EditText
    private lateinit var editManufacturer: EditText

    private fun loadCurrent() {
        if (getBackupProp(BACKUP_SUCCESS, "false") != "true") {
            return
        } else {
            editBrand.setText(android.os.Build.BRAND)
            editModel.setText(android.os.Build.MODEL)
            editProductName.setText(android.os.Build.PRODUCT)
            editDevice.setText(android.os.Build.DEVICE)
            editManufacturer.setText(android.os.Build.MANUFACTURER)
        }
    }

    private fun setDefault() {
        if (getBackupProp(BACKUP_SUCCESS, "false") != "true") {
            editBrand.setText(android.os.Build.BRAND)
            editModel.setText(android.os.Build.MODEL)
            editProductName.setText(android.os.Build.PRODUCT)
            editDevice.setText(android.os.Build.DEVICE)
            editManufacturer.setText(android.os.Build.MANUFACTURER)
        } else {
            editBrand.setText(getBackupProp(BACKUP_BRAND, android.os.Build.BRAND))
            editModel.setText(getBackupProp(BACKUP_MODEL, android.os.Build.MODEL))
            editProductName.setText(getBackupProp(BACKUP_PRODUCT, android.os.Build.PRODUCT))
            editDevice.setText(getBackupProp(BACKUP_DEVICE, android.os.Build.DEVICE))
            editManufacturer.setText(getBackupProp(BACKUP_MANUFACTURER, android.os.Build.MANUFACTURER))
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun backupDefault() {
        if (getBackupProp(BACKUP_SUCCESS, "false") != "true") {
            PropsUtils.setPorp(BACKUP_BRAND, android.os.Build.BRAND)
            PropsUtils.setPorp(BACKUP_MODEL, android.os.Build.MODEL)
            PropsUtils.setPorp(BACKUP_PRODUCT, android.os.Build.PRODUCT)
            PropsUtils.setPorp(BACKUP_DEVICE, android.os.Build.DEVICE)
            PropsUtils.setPorp(BACKUP_MANUFACTURER, android.os.Build.MANUFACTURER)
            PropsUtils.setPorp(BACKUP_SUCCESS, "true")
        }
    }


    private fun templateChooser() {
        var index = -1;
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle("选取内置模板")
                .setSingleChoiceItems(R.array.device_templates, index, { dialog, which ->
                    index = which
                })
                .setPositiveButton(R.string.btn_confirm, { dialog, which ->
                    val codeStr = context.resources.getStringArray(R.array.device_templates_data)[index]
                    splitCodeStr(codeStr)
                }))
    }
}
