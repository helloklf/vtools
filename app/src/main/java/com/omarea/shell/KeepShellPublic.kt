package com.omarea.shell

/**
 * Created by Hello on 2018/01/23.
 */
object KeepShellPublic {
    private var keepShell: KeepShell? = null

    //执行脚本
    internal fun doCmdSync(cmd: String): String {
        if (keepShell == null) {
            keepShell = KeepShell()
        }
        return keepShell!!.doCmdSync(cmd)
    }
}
