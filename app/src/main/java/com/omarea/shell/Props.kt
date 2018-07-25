package com.omarea.shell

/**
 * Created by Hello on 2017/8/8.
 */

object Props {
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return 内容
     */
    fun getProp(propName: String): String {
        return KeepShellSync.doCmdSync("getprop \"$propName\"")
    }

    fun setPorp(propName: String, value: String): Boolean {
        return KeepShellSync.doCmdSync("setprop \"$propName\" \"$value\"") != "error"
    }
}
