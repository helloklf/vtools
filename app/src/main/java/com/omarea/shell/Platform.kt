package com.omarea.shell

/**
 * 读取CPU信息
 * Created by helloklf on 2017/6/3.
 */

class Platform {
    //获取CPU型号，如msm8996
    fun getCPUName(): String {
        val cpu = Props.getProp("ro.board.platform")

        return cpu
    }
}
