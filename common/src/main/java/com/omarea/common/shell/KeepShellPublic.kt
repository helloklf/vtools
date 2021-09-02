package com.omarea.common.shell

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

    fun destroyInstance(key: String) {
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

    fun destroyAll() {
        synchronized(keepShells) {
            while (keepShells.isNotEmpty()) {
                val key = keepShells.keys.first()
                val keepShell = keepShells.get(key)!!
                keepShells.remove(key)
                keepShell.tryExit()
            }
        }
    }

    public val defaultKeepShell = KeepShell()
    public val secondaryKeepShell = KeepShell()

    fun getDefaultInstance(): KeepShell {
        return if (defaultKeepShell.isIdle || !secondaryKeepShell.isIdle) {
            defaultKeepShell
        } else {
            secondaryKeepShell
        }
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
        return defaultKeepShell.checkRoot()
    }

    fun tryExit() {
        defaultKeepShell.tryExit()
        secondaryKeepShell.tryExit()
    }
}
