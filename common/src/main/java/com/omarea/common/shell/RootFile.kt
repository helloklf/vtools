package com.omarea.common.shell

import android.util.Log
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

    // 通过MD5比对两个文件是否相同
    fun fileEquals(path1: String, path2: String): Boolean {
        if (path1.equals(path2)) {
            return true
        }
        return KeepShellPublic.doCmdSync("if [[ -f \"$path1\" ]] && [[ -f \"$path2\" ]]; then\nif [[ `md5sum -b \"$path1\"` = `md5sum -b \"$path2\"` ]]; then\n echo 1\nfi\nfi").equals("1")
    }

    // 处理像 "drwxrwx--x   3 root     root         4096 1970-07-14 17:13 vendor_de/" 这样的数据行
    private fun shellFileInfoRow(row: String, parent: String): RootFileInfo? {
        if (row.startsWith("total ")) {
            return null
        }

        try {
            val file = RootFileInfo()

            val columns = row.trim().split(" ");
            val size = columns[0]
            file.fileSize = size.toLong() * 1024;

            //  8 /data/adb/modules/scene_systemless/ => /data/adb/modules/scene_systemless/
            val fileName = row.substring(row.indexOf(size) + size.length + 1);

            if (fileName == "./" || fileName == "../") {
                return null
            }

            // -F  append /dir *exe @sym |FIFO

            if (fileName.endsWith("/")) {
                file.filePath = fileName.substring(0, fileName.length - 1)
                file.isDirectory = true
            } else if (fileName.endsWith("@")) {
                file.filePath = fileName.substring(0, fileName.length - 1)
            } else if (fileName.endsWith("|")) {
                file.filePath = fileName.substring(0, fileName.length - 1)
            } else if (fileName.endsWith("*")) {
                file.filePath = fileName.substring(0, fileName.length - 1)
            } else {
                file.filePath = fileName
            }

            file.parentDir = parent

            return file
        } catch (ex: Exception) {
            return null
        }
    }

    fun list(path: String): ArrayList<RootFileInfo> {
        val absPath = if (path.endsWith("/")) path.subSequence(0, path.length - 1).toString() else path
        val files = ArrayList<RootFileInfo>()
        if (dirExists(absPath)) {
            val outputInfo = KeepShellPublic.doCmdSync("busybox ls -1Fs \"$absPath\"")
            Log.d(">>>> files", outputInfo)
            if (outputInfo != "error") {
                val rows = outputInfo.split("\n")
                for (row in rows) {
                    val file = shellFileInfoRow(row, absPath)
                    if (file != null) {
                        files.add(file)
                    } else {
                        Log.e(">>>> Scene", "MapDirError Row -> " + row)
                    }
                }
            }
        } else {
            Log.e(">>>> dir lost", absPath)
        }

        return files
    }

    fun fileInfo(path: String): RootFileInfo? {
        val absPath = if (path.endsWith("/")) path.subSequence(0, path.length - 1).toString() else path
        val outputInfo = KeepShellPublic.doCmdSync("busybox ls -1dFs \"$absPath\"")
        Log.d(">>>> file", outputInfo)
        if (outputInfo != "error") {
            val rows = outputInfo.split("\n")
            for (row in rows) {
                val file = shellFileInfoRow(row, absPath)
                if (file != null) {
                    file.filePath = absPath.substring(absPath.lastIndexOf("/") + 1)
                    file.parentDir = absPath.substring(0, absPath.lastIndexOf("/"))
                    return file
                } else {
                    Log.e(">>>> Scene", "MapDirError Row -> " + row)
                }
            }
        }

        return null
    }
}
