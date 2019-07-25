package com.omarea.shell_utils

/**
 * 读取CPU信息
 * Created by helloklf on 2017/6/3.
 */

class PlatformUtils {
    //获取CPU型号，如msm8996
    fun getCPUName(): String {
        val cpu = PropsUtils.getProp("ro.board.platform")

        return cpu
    }
}
