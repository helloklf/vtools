package com.omarea.vtools.addin

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.PlatformUtils
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R

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
        val arr = arrayOf("移除温控文件（需要重启）", "恢复温控文件（需要重启）", "临时关闭温控（重启失效）")
        var index = 0
        DialogHelper.animDialog(AlertDialog.Builder(context)
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
                }))
    }

    fun miuiSetThermalNo() {
        val cpuName = PlatformUtils().getCPUName().replace("msm", "")
        var nolimits = ""
        var baseName = ""

        if (RootFile.fileExists("/vendor/etc/thermal-engine-${cpuName}.conf") && RootFile.fileExists("/vendor/etc/thermal-engine-${cpuName}-nolimits.conf")) {
            nolimits = "/vendor/etc/thermal-engine-${cpuName}-nolimits.conf"
            baseName = "/vendor/etc/thermal-engine-${cpuName}"
        } else if (RootFile.fileExists("/vendor/etc/thermal-engine-nolimits.conf")) {
            nolimits = "/vendor/etc/thermal-engine-nolimits.conf"
            baseName = "/vendor/etc/thermal-engine"
        } else if (RootFile.fileExists("/system/etc/thermal-engine-nolimits.conf")) {
            nolimits = "/system/etc/thermal-engine-nolimits.conf"
            baseName = "/system/etc/thermal-engine"
        } else if (RootFile.fileExists("/system/etc/thermal-engine-${cpuName}.conf") && RootFile.fileExists("/system/etc/thermal-engine-${cpuName}-nolimits.conf")) {
            nolimits = "/system/etc/thermal-engine-${cpuName}-nolimits.conf"
            baseName = " /system/etc/thermal-engine-${cpuName}"
        } else {
            Toast.makeText(context, "暂不支持你当前设备或系统！", Toast.LENGTH_LONG).show()
        }
        if (nolimits.length > 0 && baseName.length > 0) {
            replaceThermalConfig(nolimits, baseName)
        }
    }

    private fun replaceThermalConfig(nolimits: String, baseName: String) {
        if (com.omarea.common.shared.MagiskExtend.moduleInstalled()) {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("确定")
                    .setMessage("本次操作将通过Magisk覆盖系统文件，需要重启后生效！")
                    .setPositiveButton(R.string.btn_confirm, { _, _ ->
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-normal.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-camera.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-class0.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-high.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-map.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-phone.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-pubgmhd.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-sgame.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-tgame.conf", nolimits)
                        com.omarea.common.shared.MagiskExtend.replaceSystemFile("${baseName}-extreme.conf", nolimits)
                        KeepShellPublic.doCmdSync("rm -rf /data/thermal")

                        Toast.makeText(context, "已通过Magisk更改参数，请重启手机~", Toast.LENGTH_SHORT).show()
                    })
                    .setNegativeButton(R.string.btn_cancel, { _, _ ->
                    }))
        } else {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("确定")
                    .setMessage("这个操作是永久性的，暂时没有做备份还原功能，如果你需要恢复之前的温控，则需要重新输入ROM（不需要清除数据），手动删除/data/thermal和/data/vendor/thermal目录并重启手机！\n\n操作完后，请重启手机！")
                    .setPositiveButton(R.string.btn_confirm, { _, _ ->
                        command = StringBuilder()
                                .append(CommonCmds.MountSystemRW)
                                .append(CommonCmds.MountVendorRW)
                                .append("function cp664()\n{\n cp $1 $2\n chmod 664 $2\n}\n\n")
                                .append("\ncp664 $nolimits ${baseName}.conf")
                                .append("\ncp664 $nolimits ${baseName}-normal.conf")
                                .append("\ncp664 $nolimits ${baseName}-camera.conf")
                                .append("\ncp664 $nolimits ${baseName}-class0.conf")
                                .append("\ncp664 $nolimits ${baseName}-high.conf")
                                .append("\ncp664 $nolimits ${baseName}-map.conf")
                                .append("\ncp664 $nolimits ${baseName}-phone.conf")
                                .append("\ncp664 $nolimits ${baseName}-pubgmhd.conf")
                                .append("\ncp664 $nolimits ${baseName}-sgame.conf")
                                .append("\ncp664 $nolimits ${baseName}-tgame.conf")
                                .append("\ncp664 $nolimits ${baseName}-extreme.conf")
                                .append("\nrm -rf /data/thermal")
                                .append("\nrm -rf /data/vendor/thermal")
                                .append("\n")
                                .toString()
                        super.run()
                    })
                    .setNegativeButton(R.string.btn_cancel, { _, _ ->
                    }))
        }
    }

    private fun removeThermal() {
        if (com.omarea.common.shared.MagiskExtend.moduleInstalled()) {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("确定")
                    .setMessage("本次操作将通过Magisk覆盖系统文件，需要重启后生效！")
                    .setPositiveButton(R.string.btn_confirm, { _, _ ->
                        com.omarea.common.shared.MagiskExtend.deleteSystemPath("/system/vendor/bin/thermal-engine")
                        com.omarea.common.shared.MagiskExtend.deleteSystemPath("/system/vendor/lib64/libthermalclient.so")
                        com.omarea.common.shared.MagiskExtend.deleteSystemPath("/system/vendor/lib64/libthermalioctl.so")
                        com.omarea.common.shared.MagiskExtend.deleteSystemPath("/system/vendor/lib/libthermalclient.so")
                    })
                    .setNegativeButton(R.string.btn_cancel, { _, _ ->
                    }))
        } else {
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
            if (com.omarea.common.shared.MagiskExtend.moduleInstalled()) {
                com.omarea.common.shared.MagiskExtend.cancelReplace("/system/vendor/bin/thermal-engine")
                com.omarea.common.shared.MagiskExtend.cancelReplace("/system/vendor/lib64/libthermalclient.so")
                com.omarea.common.shared.MagiskExtend.cancelReplace("/system/vendor/lib64/libthermalioctl.so")
                com.omarea.common.shared.MagiskExtend.cancelReplace("/system/vendor/lib/libthermalclient.so")
            }

            super.run()
        }
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
                .append("\nstop thermanager")
                .append("\nstop thermald")
                .append("\nstop mpdecision")
                .append("\nstop thermal-engine")
                .append("\necho 0 > /sys/module/msm_thermal/core_control/enabled")
                .append("\necho 0 > /sys/module/msm_thermal/vdd_restriction/enabled")
                .append("\necho N > /sys/module/msm_thermal/parameters/enabled")
                .append("\nkillall -9 vendor.qti.hardware.perf@1.0-service")
                .append("\n")
                .toString()

        super.run()
    }
}
