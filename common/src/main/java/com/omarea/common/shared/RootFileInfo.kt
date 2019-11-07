package com.omarea.common.shared

import com.omarea.common.shell.RootFile

class RootFileInfo {
    constructor()
    constructor(path: String) {
        val file = RootFile.fileInfo(path)
        if (file != null) {
            this.permissions = file.permissions
            this.parentDir = file.parentDir
            this.filePath = file.filePath
            this.isDirectory = file.isDirectory
            this.inodeCount = file.inodeCount
            this.fileSize = file.fileSize
            this.owner = file.owner
            this.ownerGroup = file.ownerGroup
            this.lastModifyDateTime = file.lastModifyDateTime
        }
    }

    var permissions: String = ""
    var parentDir: String = ""
    var filePath: String = ""
    var isDirectory: Boolean = false
    var inodeCount = 0
    var fileSize: Long = 0
    var owner: String = ""
    var ownerGroup: String = ""
    var lastModifyDateTime: String = ""
    var executable: Boolean = false
    var softLink = ""

    val fileName: String
        get() {
            if (filePath.endsWith("/")) {
                return filePath.substring(0, filePath.length - 1)
            }
            return filePath
        }

    val absolutePath: String
        get() = parentDir + "/" + fileName


    public fun exists(): Boolean {
        return RootFile.itemExists(this.absolutePath)
    }

    public fun isFile(): Boolean {
        return !isDirectory
    }

    public fun getParent(): String {
        return this.parentDir
    }

    public fun getName(): String {
        return this.fileName
    }

    public fun listFiles(): ArrayList<RootFileInfo> {
        if (this.isDirectory) {
            if (this.softLink != "") {
                return RootFile.list(this.softLink)
            } else {
                return RootFile.list(this.absolutePath)
            }
        }
        return ArrayList()
    }

    fun length(): Long {
        return this.fileSize
    }
}
