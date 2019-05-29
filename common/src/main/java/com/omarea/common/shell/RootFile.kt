package com.omarea.common.shell

/**
 * Created by Hello on 2018/07/06.
 */

object RootFile {
    fun itemExists(path: String): Boolean {
        return KeepShellPublic.doCmdSync("if [[ -e \"$path\" ]]; then echo 1; fi;").equals("1")
    }

    fun fileExists(path: String): Boolean {
        return KeepShellPublic.doCmdSync("if [[ -f \"$path\" ]]; then echo 1; fi;").equals("1")
    }

    fun fileNotEmpty(path: String): Boolean {
        return KeepShellPublic.doCmdSync("if [[ -f \"$path\" ]] && [[ -s \"$path\" ]]; then echo 1; fi;").equals("1")
    }

    fun dirExists(path: String): Boolean {
        return KeepShellPublic.doCmdSync("if [[ -d \"$path\" ]]; then echo 1; fi;").equals("1")
    }

    fun deleteDirOrFile(path: String) {
        KeepShellPublic.doCmdSync("rm -rf \"$path\"")
    }

    fun fileSize(path: String): Int {
        val fileInfo = KeepShellPublic.doCmdSync("if [[ -e \"$path\" ]]; then ls -l \"" + path + "\"; fi;")
        val fileInfos = fileInfo.split(" ")
        if (fileInfos.size > 4) {
            try {
                return fileInfos[4].toInt()
            } catch (ex: Exception) {
                return -1
            }
        }
        return 0
    }
}
