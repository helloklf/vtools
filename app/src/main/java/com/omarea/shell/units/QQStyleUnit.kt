package com.omarea.shell.units

import com.omarea.shared.Consts
import com.omarea.shell.SysUtils
import java.util.*

/**
 * Created by Hello on 2017/11/01.
 */

class QQStyleUnit {
    fun DisableQQStyle(): Boolean {
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

        return SysUtils.executeRootCommand(commands)
    }

    fun RestoreQQStyle(): Boolean {
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

        return SysUtils.executeRootCommand(commands)
    }
}
