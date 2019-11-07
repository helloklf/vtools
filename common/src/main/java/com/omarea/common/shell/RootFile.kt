package com.omarea.common.shell

import com.omarea.common.shared.RootFileInfo

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

    // 处理像 "drwxrwx--x   3 root     root         4096 1970-07-14 17:13 vendor_de/" 这样的数据行
    private fun shellFileInfoRow(row: String, parent: String): RootFileInfo? {
        if (row.startsWith("total ")) {
            return null
        }

        val file = RootFileInfo()

        val buffer = StringBuffer()
        var spaceCount = 0
        for (i in 0 until row.length) {
            if (spaceCount < 7 && row[i] == ' ') {
                if (buffer.length > 0) {
                    when (spaceCount) {
                        0 -> {
                            file.permissions = buffer.toString()
                        }
                        1 -> {
                            file.inodeCount = buffer.toString().toInt()
                        }
                        2 -> {
                            file.owner = buffer.toString()
                        }
                        3 -> {
                            file.ownerGroup = buffer.toString()
                        }
                        4 -> {
                            file.fileSize = buffer.toString().toLong()
                        }
                        5 -> {
                            file.lastModifyDateTime = buffer.toString()
                        }
                        6 -> {
                            file.lastModifyDateTime += buffer
                        }
                    }
                    spaceCount++
                    buffer.delete(0, buffer.length)
                }
            } else {
                buffer.append(row[i])
            }
        }
        val fileName = buffer.toString()

        if (fileName == "./" || fileName == "../") {
            return null
        }

        if (fileName.endsWith("/")) {
            file.filePath = fileName.substring(0, fileName.length - 1)
            file.isDirectory = true
        } else if (fileName.endsWith("*")) {
            file.filePath = fileName.substring(0, fileName.length - 1)
            file.executable = true
        } else if (fileName.contains(" -> ")) {
            val index = fileName.indexOf(" -> ")
            file.filePath = fileName.substring(0, index)
            file.softLink = fileName.substring(index + 4)
            if (file.softLink.endsWith("@")) {
                file.softLink = file.softLink.substring(0, file.softLink.length - 1)
            }
            if (RootFile.dirExists(file.softLink)) {
                file.isDirectory = true
            } else if (RootFile.fileExists(file.softLink)) {
                file.isDirectory = false
            } else {
                // 软链无效！
                // return null
            }
        } else {
            file.filePath = fileName
        }

        file.parentDir = parent

        return file
    }

    fun list(path: String): ArrayList<RootFileInfo> {
        val absPath = if (path.endsWith("/")) path.subSequence(0, path.length - 1).toString() else path
        val files = ArrayList<RootFileInfo>()
        if (dirExists(absPath)) {
            val outputInfo = KeepShellPublic.doCmdSync("ls -laF \"$absPath\"")
            if (outputInfo != "error") {
                val rows = outputInfo.split("\n")
                for (row in rows) {
                    val file = shellFileInfoRow(row, absPath)
                    if (file != null) {
                        files.add(file)
                    }
                }
            }
        }

        return files
    }

    fun fileInfo(path: String): RootFileInfo? {
        val absPath = if (path.endsWith("/")) path.subSequence(0, path.length - 1).toString() else path
        val outputInfo = KeepShellPublic.doCmdSync("ls -ldF \"$absPath\"")
        if (outputInfo != "error") {
            val rows = outputInfo.split("\n")
            for (row in rows) {
                val file = shellFileInfoRow(row, absPath)
                if (file != null) {
                    file.filePath = absPath.substring(absPath.lastIndexOf("/") + 1)
                    file.parentDir = absPath.substring(0, absPath.lastIndexOf("/"))
                    return file
                }
            }
        }

        return null
    }
}
