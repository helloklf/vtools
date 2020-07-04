package com.omarea.krscript.config

import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.krscript.FileOwner
import java.io.File
import java.io.InputStream

class PathAnalysis(private var context: Context, private var parentDir: String = "") {
    private val ASSETS_FILE = "file:///android_asset/"

    // 解析路径时自动获得
    private var currentAbsPath: String = ""

    fun getCurrentAbsPath(): String {
        return currentAbsPath
    }

    fun parsePath(filePath: String): InputStream? {
        try {
            if (filePath.startsWith(ASSETS_FILE)) {
                currentAbsPath = filePath
                return context.assets.open(filePath.substring(ASSETS_FILE.length))
            } else {
                return getFileByPath(filePath)
            }
        } catch (ex: Exception) {
            return null
        }
    }

    // TODO:处理 ../ 、 ./
    private fun pathConcat(parent: String, target: String): String {
        val isAssets = parent.startsWith(ASSETS_FILE)
        val parentDir = if (isAssets) parent.substring(ASSETS_FILE.length) else parent
        val parentSlices = ArrayList(parentDir.split("/"))
        if (target.startsWith("../") && parentSlices.size > 0) {
            val targetSlices = ArrayList(target.split("/"))
            while (true) {
                val step = targetSlices.firstOrNull()
                if (step != null && step == ".." && parentSlices.size > 0) {
                    parentSlices.removeAt(parentSlices.size - 1)
                    targetSlices.removeAt(0)
                } else {
                    break
                }
            }
            return pathConcat((if (isAssets) ASSETS_FILE  else "" )+ parentSlices.joinToString("/"), targetSlices.joinToString("/"))
        }

        return (if (isAssets) ASSETS_FILE  else "" )+ ( when {
            !(parentDir.isEmpty() || parentDir.endsWith("/")) -> parentDir + "/"
            else -> parentDir
        } + (if (target.startsWith("./")) target.substring(2) else target))
    }

    private fun useRootOpenFile(filePath: String): InputStream? {
        if (RootFile.fileExists(filePath)) {
            val dir = File(FileWrite.getPrivateFilePath(context, "kr-script"))
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val cachePath = FileWrite.getPrivateFilePath(context, "kr-script/outside_file.cache")
            val fileOwner = FileOwner(context).fileOwner
            KeepShellPublic.doCmdSync(
                    "cp -f \"$filePath\" \"$cachePath\"\n" +
                            "chmod 777 \"$cachePath\"\n" +
                            "chown $fileOwner:$fileOwner \"$cachePath\"\n")
            File(cachePath).run {
                if (exists() && canRead()) {
                    return inputStream()
                }
            }
        }
        return null
    }

    // 在assets里查找文件
    private fun findAssetsResource(filePath: String): InputStream? {
        // 解析成绝对路径
        val relativePath = pathConcat(parentDir, filePath)
        try {
            try {
                // 首先在assets里查找相对路径
                val simplePath = relativePath.substring(ASSETS_FILE.length)
                context.assets.open(simplePath).run {
                    currentAbsPath = relativePath
                    return this
                }
            } catch (ex: java.lang.Exception) {
                // 然后再尝试再assets里查找绝对路径
                context.assets.open(filePath).run {
                    currentAbsPath = ASSETS_FILE + filePath
                    return this
                }
            }
        } catch (ex: java.lang.Exception) {
            return null
        }
    }

    // 在磁盘上查找文件
    private fun findDiskResource(filePath: String): InputStream? {
        if (parentDir.isNotEmpty()) {
            // 解析成绝对路径
            val relativePath = pathConcat(parentDir, filePath)
            // 尝试使用普通权限读取文件
            File(relativePath).run {
                if (exists() && canRead()) {
                    currentAbsPath = absolutePath
                    return inputStream()
                }
            }
            useRootOpenFile(relativePath)?.run {
                return this
            }
        }

        // 路径相对于当前配置文件没找到文件的话，继续查找相对于数据文件根目录的文件
        val privatePath = File( pathConcat(FileWrite.getPrivateFileDir(context), filePath)).absolutePath
        File(privatePath).run {
            if (exists() && canRead()) {
                currentAbsPath = absolutePath
                return inputStream()
            }
        }
        useRootOpenFile(privatePath)?.run {
            return this
        }

        return null
    }

    private fun getFileByPath(filePath: String): InputStream? {
        try {
            if (filePath.startsWith("/")) {
                currentAbsPath = filePath
                val javaFileInfo = File(filePath)
                if (javaFileInfo.exists() && javaFileInfo.canRead()) {
                    return javaFileInfo.inputStream()
                } else {
                    return useRootOpenFile(filePath)
                }
            } else {
                // 如果当前配置文件来源于 assets，则查找依赖资源时也只去assets查找
                if (parentDir.isNotEmpty() && parentDir.startsWith(ASSETS_FILE)) {
                    return findAssetsResource(filePath)
                } else {
                    return findDiskResource(filePath)
                }
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return null
    }
}
