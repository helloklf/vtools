package com.omarea.vboot.addin

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.omarea.shared.Consts
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
                .append(Consts.MountSystemRW)
                .append(Consts.RMThermal)
                .toString()

        super.run()
    }

    private fun resumeThermal() {
        if (File("/system/vendor/bin/thermal-engine").exists()) {
            Toast.makeText(context, "你不需要此操作，温控文件正在正常使用，如果无效请重启手机！", Toast.LENGTH_SHORT).show()
            return
        }
        command = StringBuilder()
                .append(Consts.MountSystemRW)
                .append(Consts.ResetThermal)
                .toString()

        super.run()
    }

    private fun closeThermal() {
        if (!isSupprt()) {
            return
        }
        command = StringBuilder()
                .append("stop thermald;")
                .append("stop mpdecision;")
                .append("stop thermal-engine;")
                .append("echo 0 > /sys/module/msm_thermal/core_control/enabled;")
                .append("echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled;")
                .append("echo N > /sys/module/msm_thermal/parameters/enabled;")
                .toString()

        super.run()
    }
}
