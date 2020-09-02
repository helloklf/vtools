package com.omarea.library.shell

import com.omarea.common.shell.KeepShellPublic

/**
 * Created by Hello on 2017/8/8.
 */

object PropsUtils {
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return 内容
     */
    fun getProp(propName: String): String {
        return KeepShellPublic.doCmdSync("getprop \"$propName\"")
    }

    fun setPorp(propName: String, value: String): Boolean {
        return KeepShellPublic.doCmdSync("setprop \"$propName\" \"$value\"") != "error"
    }
}
