package com.omarea.common.shell

import android.util.Log

/**
 * Created by Hello on 2018/01/23.
 */
object KeepShellPublic {
    private val keepShells = HashMap<String, KeepShell>()

    fun getInstance(key: String, rootMode: Boolean): KeepShell {
        synchronized(keepShells) {
            if (!keepShells.containsKey(key)) {
                keepShells.put(key, KeepShell(rootMode))
            }
            return keepShells.get(key)!!
        }
    }

    fun destoryInstance(key: String) {
        synchronized(keepShells) {
            if (!keepShells.containsKey(key)) {
                return
            } else {
                val keepShell = keepShells.get(key)!!
                keepShells.remove(key)
                keepShell.tryExit()
            }
        }
    }

    fun destoryAll() {
        synchronized(keepShells) {
            while (keepShells.isNotEmpty()) {
                val key = keepShells.keys.first()
                val keepShell = keepShells.get(key)!!
                keepShells.remove(key)
                keepShell.tryExit()
            }
        }
    }

    private var defaultKeepShell: KeepShell? = null

    fun getDefaultInstance(): KeepShell {
        if (defaultKeepShell == null) {
            defaultKeepShell = KeepShell()
        }
        return defaultKeepShell!!
    }

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
        return getDefaultInstance().doCmdSync(cmd)
    }

    //执行脚本
    fun checkRoot(): Boolean {
        return getDefaultInstance().checkRoot()
    }

    fun tryExit() {
        if (defaultKeepShell != null) {
            defaultKeepShell!!.tryExit()
            defaultKeepShell = null
        }
    }
}
