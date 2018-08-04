package com.omarea.vtools.addin

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.omarea.shared.CommonCmds
import java.io.File

/**
 * Created by Hello on 2018/03/22.
 */

class ThermalAddin(private var context: Context) : AddinBase(context) {
    private fun isSupprt(): Boolean {
        if (File("/system/vendor/bin/thermal-engine").exists() || File("/system/vendor/bin/thermal-engine.bak").exists()) {
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

    private fun removeThermal() {
        if (File("/system/vendor/bin/thermal-engine.bak").exists()) {
            Toast.makeText(context, "你已执行过这个操作，不需要再次执行，如果未生效请重启手机！", Toast.LENGTH_SHORT).show()
            return
        }
        command = StringBuilder()
                .append(CommonCmds.MountSystemRW)
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
        if (File("/system/vendor/bin/thermal-engine").exists()) {
            Toast.makeText(context, "你不需要此操作，温控文件正在正常使用，如果无效请重启手机！", Toast.LENGTH_SHORT).show()
            return
        }
        command = StringBuilder()
                .append(CommonCmds.MountSystemRW)
                .append("cp /system/vendor/bin/thermal-engine.bak /system/vendor/bin/thermal-engine\n" +
                        "chmod 644 /system/vendor/bin/thermal-engine\n" +
                        "rm -f /system/vendor/bin/thermal-engine.bak\n" +

                        "cp /system/vendor/lib64/libthermalclient.so.bak /system/vendor/lib64/libthermalclient.so\n" +
                        "chmod 644 /system/vendor/lib64/libthermalclient.so\n" +
                        "rm -f /system/vendor/lib64/libthermalclient.so.bak\n" +

                        "cp /system/vendor/lib64/libthermalioctl.so.bak /system/vendor/lib64/libthermalioctl.so\n" +
                        "chmod 644 /system/vendor/lib64/libthermalioctl.so\n" +
                        "rm -f /system/vendor/lib64/libthermalioctl.so.bak\n" +

                        "cp /system/vendor/lib/libthermalclient.so.bak /system/vendor/lib/libthermalclient.so\n" +
                        "chmod 644 /system/vendor/lib/libthermalclient\n" +
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
