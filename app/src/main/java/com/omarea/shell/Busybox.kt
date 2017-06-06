package com.omarea.shell

/**
 * Created by helloklf on 2017/6/3.
 */

class Busybox {
    //是否已经安装busybox
    fun IsBusyboxInstalled(): Boolean {
        try {
            Runtime.getRuntime().exec("busybox").destroy()
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
