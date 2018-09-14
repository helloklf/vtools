package com.omarea.vtools.addin

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.shell.Platform
import com.omarea.shell.RootFile
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.dpi_input.view.*
import java.io.File

/**
 * Created by Hello on 2018/03/22.
 */

class ThermalAddin(private var context: Context) : AddinBase(context) {
    private fun isSupprt(): Boolean {
        if (RootFile.fileExists("/system/vendor/bin/thermal-engine") || RootFile.fileExists("/system/vendor/bin/thermal-engine.bak")) {
            return true
        } else {
            Toast.makeText(context, "该功能暂不支持您的设备！", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    fun showOption() {
        if (!isSupprt()) {
            return
        }
        val arr = arrayOf("移除温控文件（需要重启）", "恢复温控文件（需要重启）", "临时关闭温控")
        var index = 0
        AlertDialog.Builder(context)
                .setTitle("请选择操作")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    when (index) {
                        0 -> removeThermal()
                        1 -> resumeThermal()
                        2 -> closeThermal()
                    }
                })
                .create().show()
    }

    fun miuiSetThermalNo () {
        val cpuName = Platform().getCPUName().replace("msm", "")
        if (RootFile.fileExists("/vendor/etc/thermal-engine-${cpuName}.conf") && RootFile.fileExists("/vendor/etc/thermal-engine-${cpuName}-nolimits.conf")) {
            AlertDialog.Builder(context)
                    .setTitle("确定")
                    .setMessage("这个操作是永久性的，暂时没有做备份还原功能，如果你需要恢复之前的温控，则需要重新输入ROM（不需要清除数据），手动删除/data/thermal目录并重启手机！\n\n操作完后，请重启手机！")
                    .setPositiveButton(R.string.btn_confirm, {
                        _, _ ->
                        command = StringBuilder()
                                .append(CommonCmds.MountSystemRW)
                                .append(CommonCmds.MountVendorRW)
                                .append("\ncp /vendor/etc/thermal-engine-${cpuName}-nolimits.conf /vendor/etc/thermal-engine-${cpuName}.conf")
                                .append("\ncp /vendor/etc/thermal-engine-${cpuName}-nolimits.conf /vendor/etc/thermal-engine-${cpuName}-camera.conf")
                                .append("\ncp /vendor/etc/thermal-engine-${cpuName}-nolimits.conf /vendor/etc/thermal-engine-${cpuName}-class0.conf")
                                .append("\ncp /vendor/etc/thermal-engine-${cpuName}-nolimits.conf /vendor/etc/thermal-engine-${cpuName}-high.conf")
                                .append("\ncp /vendor/etc/thermal-engine-${cpuName}-nolimits.conf /vendor/etc/thermal-engine-${cpuName}-map.conf")
                                .append("\ncp /vendor/etc/thermal-engine-${cpuName}-nolimits.conf /vendor/etc/thermal-engine-${cpuName}-phone.conf")
                                .append("\ncp /vendor/etc/thermal-engine-${cpuName}-nolimits.conf /vendor/etc/thermal-engine-${cpuName}-pubgmhd.conf")
                                .append("\ncp /vendor/etc/thermal-engine-${cpuName}-nolimits.conf /vendor/etc/thermal-engine-${cpuName}-sgame.conf")
                                .append("\nrm -rf /data/thermal")
                                .append("\n")
                                .toString()
                        super.run()
                    })
                    .setNegativeButton(R.string.btn_cancel, {
                        _, _ ->
                    })
                    .create()
                    .show()
        } else if (RootFile.fileExists("/system/etc/thermal-engine-${cpuName}.conf") && RootFile.fileExists("/system/etc/thermal-engine-${cpuName}-nolimits.conf")) {
            AlertDialog.Builder(context)
                    .setTitle("确定")
                    .setMessage("这个操作是永久性的，暂时没有做备份还原功能，如果你需要恢复之前的温控，则需要重新输入ROM（不需要清除数据），手动删除/data/thermal目录并重启手机！\n\n操作完后，请重启手机！")
                    .setPositiveButton(R.string.btn_confirm, {
                        _, _ ->
                        command = StringBuilder()
                                .append(CommonCmds.MountSystemRW)
                                .append(CommonCmds.MountVendorRW)
                                .append("\ncp /system/etc/thermal-engine-${cpuName}-nolimits.conf /system/etc/thermal-engine-${cpuName}.conf")
                                .append("\ncp /system/etc/thermal-engine-${cpuName}-nolimits.conf /system/etc/thermal-engine-${cpuName}-camera.conf")
                                .append("\ncp /system/etc/thermal-engine-${cpuName}-nolimits.conf /system/etc/thermal-engine-${cpuName}-class0.conf")
                                .append("\ncp /system/etc/thermal-engine-${cpuName}-nolimits.conf /system/etc/thermal-engine-${cpuName}-high.conf")
                                .append("\ncp /system/etc/thermal-engine-${cpuName}-nolimits.conf /system/etc/thermal-engine-${cpuName}-map.conf")
                                .append("\ncp /system/etc/thermal-engine-${cpuName}-nolimits.conf /system/etc/thermal-engine-${cpuName}-phone.conf")
                                .append("\ncp /system/etc/thermal-engine-${cpuName}-nolimits.conf /system/etc/thermal-engine-${cpuName}-pubgmhd.conf")
                                .append("\ncp /system/etc/thermal-engine-${cpuName}-nolimits.conf /system/etc/thermal-engine-${cpuName}-sgame.conf")
                                .append("\nrm -rf /data/thermal")
                                .append("\n")
                                .toString()
                        super.run()
                    })
                    .setNegativeButton(R.string.btn_cancel, {
                        _, _ ->
                    })
                    .create()
                    .show()
        } else {
            Toast.makeText(context, "暂不支持你当前设备或系统！", Toast.LENGTH_LONG).show()
        }
    }

    private fun removeThermal() {
        if (RootFile.fileExists("/system/vendor/bin/thermal-engine.bak")) {
            Toast.makeText(context, "你已执行过这个操作，不需要再次执行，如果未生效请重启手机！", Toast.LENGTH_SHORT).show()
            return
        }
        command = StringBuilder()
                .append(CommonCmds.MountSystemRW)
                .append(CommonCmds.MountVendorRW)
                .append("cp /system/vendor/bin/thermal-engine /system/vendor/bin/thermal-engine.bak\n" +
                        "rm -f /system/vendor/bin/thermal-engine\n" +

                        "cp /system/vendor/lib64/libthermalclient.so /system/vendor/lib64/libthermalclient.so.bak\n" +
                        "rm -f /system/vendor/lib64/libthermalclient.so\n" +

                        "cp /system/vendor/lib64/libthermalioctl.so /system/vendor/lib64/libthermalioctl.so.bak\n" +
                        "rm -f /system/vendor/lib64/libthermalioctl.so\n" +

                        "cp /system/vendor/lib/libthermalclient.so /system/vendor/lib/libthermalclient.so.bak\n" +
                        "rm -f /system/vendor/lib/libthermalclient.so\n")
                .toString()

        super.run()
    }

    private fun resumeThermal() {
        if (RootFile.fileExists("/system/vendor/bin/thermal-engine")) {
            Toast.makeText(context, "你不需要此操作，温控文件正在正常使用，如果无效请重启手机！", Toast.LENGTH_SHORT).show()
            return
        }
        command = StringBuilder()
                .append(CommonCmds.MountSystemRW)
                .append(CommonCmds.MountVendorRW)
                .append("cp /system/vendor/bin/thermal-engine.bak /system/vendor/bin/thermal-engine\n" +
                        "chmod 755 /system/vendor/bin/thermal-engine\n" +
                        "rm -f /system/vendor/bin/thermal-engine.bak\n" +

                        "cp /system/vendor/lib64/libthermalclient.so.bak /system/vendor/lib64/libthermalclient.so\n" +
                        "chmod 755 /system/vendor/lib64/libthermalclient.so\n" +
                        "rm -f /system/vendor/lib64/libthermalclient.so.bak\n" +

                        "cp /system/vendor/lib64/libthermalioctl.so.bak /system/vendor/lib64/libthermalioctl.so\n" +
                        "chmod 755 /system/vendor/lib64/libthermalioctl.so\n" +
                        "rm -f /system/vendor/lib64/libthermalioctl.so.bak\n" +

                        "cp /system/vendor/lib/libthermalclient.so.bak /system/vendor/lib/libthermalclient.so\n" +
                        "chmod 755 /system/vendor/lib/libthermalclient\n" +
                        "rm -f /system/vendor/lib/libthermalclient.so.bak\n")
                .toString()

        super.run()
    }

    private fun closeThermal() {
        if (!isSupprt()) {
            return
        }
        command = StringBuilder()
                .append("stop thermanager\n")
                .append("stop thermald\n")
                .append("stop mpdecision\n")
                .append("stop thermal-engine\n")
                .append("echo 0 > /sys/module/msm_thermal/core_control/enabled\n")
                .append("echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled\n")
                .append("echo N > /sys/module/msm_thermal/parameters/enabled\n")
                .toString()

        super.run()
    }
}
