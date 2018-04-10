package com.omarea.shell

class FileBroweser {
    private fun executeShell(cmd: String) {

    }

    /**
     * 跳转目录
     */
    fun cd(dir: String): FileBroweser {
        executeShell("cd ${dir}")

        return this
    }

    /**
     * 获取当前目录下的所有文件
     */
    fun getFiles() {
        executeShell("ls -lh .*")
    }

    fun rmFile(path: String, force: Boolean = false): FileBroweser {
        if (force)
            executeShell("rm -f ${path}")
        else
            executeShell("rm ${path}")

        return this
    }

    fun rmDir(path: String, force: Boolean = false): FileBroweser {
        if (force)
            executeShell("rm -rf ${path}")
        else
            executeShell("rm -r ${path}")

        return this
    }

    fun chmodFile(mode: String): FileBroweser {
        executeShell("chmod $mode")
        return this
    }

    /**
     *
     */
    fun chmodDir(mode: String): FileBroweser {
        executeShell("chmod -r $mode")
        return this
    }

    /**
     * 重命名文件
     */
    fun rename(originName: String, newName: String) {

    }

    /**
     * 获取当前路径
     */
    fun getPath() {

    }

    /**
     * 获取当前目录下某个文件的完整路径
     */
    fun getPath(path: String) {

    }

    /**
     * 设置文件权限
     */
    fun chownFile(group: String, user: String) {

    }

    /**
     * 设置目录权限
     */
    fun chownDir(group: String, user: String) {

    }

    /**
     * 复制文件
     */
    fun cp(from:String, to:String) {

    }

    /**
     * 移动文件
     */
    fun mv(from:String, to:String) {

    }

    /**
     * 在当前目录下创建.nomedia，避免此目录下的媒体文件被扫描
     */
    fun noMedia() {
        executeShell("echo '' > .nomedia;")
    }
}
