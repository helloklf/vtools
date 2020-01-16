package com.omarea.shell_utils

import android.content.Context
import com.omarea.common.shell.KeepShell

class SceneStandbyMode(private val context: Context, private val keepShell: KeepShell) {
    public fun on() {
        keepShell.doCmdSync("")
    }

    public fun off() {
        keepShell.doCmdSync("")
    }
}
