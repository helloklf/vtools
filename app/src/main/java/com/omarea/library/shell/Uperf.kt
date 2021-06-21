package com.omarea.library.shell

import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile

class Uperf {
    public fun installed(): Boolean {
        val moduleExists = RootFile.fileExists("/data/adb/modules/uperf/module.prop")
        if (moduleExists) {
            val result = KeepShellPublic.doCmdSync("grep 'Author: Matt Yang' /data/powercfg.sh")
            if (result.startsWith("#")) {
                return true
            }
        }
        return false
    }
}