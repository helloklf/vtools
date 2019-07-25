package com.omarea.common.shell

/**
 * Created by Hello on 2018/01/23.
 */
object KeepShellPublic {
    private var keepShell: KeepShell? = null

    fun doCmdSync(commands: List<String>): Boolean {
        val stringBuilder = StringBuilder()

        for (cmd in commands) {
            stringBuilder.append(cmd)
            stringBuilder.append("\n\n")
        }

        return doCmdSync(stringBuilder.toString()) != "error"
    }

    //执行脚本
    fun doCmdSync(cmd: String): String {
        if (keepShell == null) {
            keepShell = KeepShell()
        }
        return keepShell!!.doCmdSync(cmd)
    }

    //执行脚本
    fun checkRoot(): Boolean {
        if (keepShell == null) {
            keepShell = KeepShell()
        }
        return keepShell!!.checkRoot()
    }

    fun tryExit() {
        if (keepShell != null) {
            keepShell!!.tryExit()
            keepShell = null
        }
    }
}
