package com.omarea.shell

import android.content.Context
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * 读取CPU信息
 * Created by helloklf on 2017/6/3.
 */

class Platform {
    //获取CPU型号，如msm8996
    fun GetCPUName(): String {
        val cpu = Props.getProp("ro.board.platform")
        if( cpu==null) {
            return  ""
        } else {
            return cpu
        }
    }

    fun dynamicSupport(context: Context): Boolean {
        val cpuName = GetCPUName()
        val names = context.assets.list("")
        for (i in names.indices) {
            if (names[i].equals(cpuName)) {
                return true
            }
        }
        return false;
    }
}
