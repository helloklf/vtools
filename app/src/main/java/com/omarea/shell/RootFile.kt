package com.omarea.shell

/**
 * Created by Hello on 2018/07/06.
 */

object RootFile {
    fun itemExists(path: String): Boolean {
        return KeepShellPublic.doCmdSync("if [[ -e $path ]]; then echo 1; fi;").equals("1")
    }

    fun fileExists(path: String): Boolean {
        return KeepShellPublic.doCmdSync("if [[ -f $path ]]; then echo 1; fi;").equals("1")
    }

    fun dirExists(path: String): Boolean {
        return KeepShellPublic.doCmdSync("if [[ -d $path ]]; then echo 1; fi;").equals("1")
    }
}
