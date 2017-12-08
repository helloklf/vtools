package com.omarea.shell.units

import com.omarea.shared.Consts
import com.omarea.shell.SysUtils

import java.util.ArrayList

/**
 * Created by Hello on 2017/11/01.
 */

class FlymeUnit {
    fun StaticBlur(): Boolean {
        val commands = object : ArrayList<String>() {
            init {
                add("setprop persist.sys.static_blur_mode true")
            }
        }
        return SysUtils.executeRootCommand(commands)
    }

    fun DisableMtkLog(): Boolean {
        val commands = object : ArrayList<String>() {
            init {
                add("rm -rf ${Consts.SDCardDir}/mtklog")
                add("echo 0 > ${Consts.SDCardDir}/mtklog")
            }
        }
        return SysUtils.executeRootCommand(commands)
    }
}
