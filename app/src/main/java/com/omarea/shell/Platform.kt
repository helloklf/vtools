package com.omarea.shell

import android.content.Context

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

    //
    fun dynamicSupport(context: Context): Boolean {
        val cpuName = getCPUName()
        val names = context.assets.list("")
        for (i in names.indices) {
            if (names[i].equals(cpuName)) {
                return true
            }
        }
        return false;
    }
}
