package com.omarea.shell.units

import android.app.AlertDialog
import android.content.Context
import com.omarea.shared.Consts
import com.omarea.shell.SuDo
import java.util.*

/**
 * Created by Hello on 2017/11/01.
 */

class QQStyleUnit(var context: Context) {

    fun showOption() {
        val arr = arrayOf("禁用个性化样式", "恢复个性化样式")
        var index = 0
        AlertDialog.Builder(context)
                .setTitle("请选择操作")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    when (index) {
                        0 -> DisableQQStyle()
                        1 -> RestoreQQStyle()
                    }
                })
                .create().show()
    }

    private fun DisableQQStyle(): Boolean {
        val commands = object : ArrayList<String>() {
            init {
                add("rm -rf /storage/emulated/0/tencent/MobileQQ/.font_info\n" +
                        "echo \"\" > /storage/emulated/0/tencent/MobileQQ/.font_info\n" +
                        "rm -rf /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                        "echo \"\" > /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                        "rm -rf ${Consts.SDCardDir}/tencent/MobileQQ/.font_info\n" +
                        "echo \"\" > ${Consts.SDCardDir}/tencent/MobileQQ/.font_info\n" +
                        "rm -rf ${Consts.SDCardDir}/tencent/MobileQQ/font_info\n" +
                        "echo \"\" > ${Consts.SDCardDir}/tencent/MobileQQ/font_info\n" +
                        "rm -rf /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                        "echo \"\" > /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                        "rm -rf /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                        "echo \"\" > /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                        "rm -rf /storage/emulated/0/tencent/MobileQQ/.pendant\n" +
                        "echo \"\" > /storage/emulated/0/tencent/MobileQQ/.pendant\n" +
                        "rm -rf ${Consts.SDCardDir}/tencent/MobileQQ/.pendant\n" +
                        "echo \"\" > ${Consts.SDCardDir}/tencent/MobileQQ/.pendant\n" +
                        "pgrep com.tencent.mobileqq |xargs kill -9\n")
            }
        }

        return SuDo.execCmdSync(commands)
    }

    private fun RestoreQQStyle(): Boolean {
        val commands = object : ArrayList<String>() {
            init {
                add("rm -rf /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                        "rm -rf ${Consts.SDCardDir}/tencent/MobileQQ/font_info\n" +
                        "rm -rf /storage/emulated/0/tencent/MobileQQ/.font_info\n" +
                        "rm -rf ${Consts.SDCardDir}/tencent/MobileQQ/.font_info\n" +
                        "rm -rf /storage/emulated/0/tencent/MobileQQ/.pendant\n" +
                        "rm -rf ${Consts.SDCardDir}/tencent/MobileQQ/.pendant\n" +
                        "rm -rf /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                        "rm -rf /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                        "pgrep com.tencent.mobileqq |xargs kill -9\n")
            }
        }

        return SuDo.execCmdSync(commands)
    }
}
