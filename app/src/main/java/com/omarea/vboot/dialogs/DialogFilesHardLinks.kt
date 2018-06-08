package com.omarea.vboot.dialogs

import android.os.Handler
import com.omarea.shell.SysUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.security.MessageDigest


/**
 * Created by Hello on 2017/12/20.
 * 该类废弃，因为在java层没有权限读取底层文件
 */

class DialogFilesHardLinks(private var handler: Handler, private var rootDir: String = "/raw/data/multiboot") {
    //检查文件列表
    fun checkFileList() {
        Thread(Runnable {
            try {
                val dirs = getAppDirs()
                val apkfiles = getApkFiles(dirs)
                val fileGroups = groupByMd5(apkfiles)
                val inodeGroups = groupByInode(fileGroups)

                handler.sendMessage(handler.obtainMessage(1, inodeGroups))
            } catch (ex: Exception) {
                handler.sendMessage(handler.obtainMessage(-1))
                ex.stackTrace
            }
        }).start()
    }

    //获取系统目录
    private fun getAppDirs(): ArrayList<File> {
        val root = File(rootDir)
        val dirs = root.listFiles({ pathname: File? ->
            pathname != null && pathname.isDirectory && pathname.canRead() && File(pathname.absolutePath + "/data/app").exists()
        })
        val appDirs = ArrayList<File>()
        for (dir in dirs) {
            appDirs.add(File(dir.absolutePath + "/data/app"))
        }
        return appDirs
    }

    //获取多个应用安装目录下所有apk文件
    private fun getApkFiles(dirs: ArrayList<File>): ArrayList<File> {
        val array = ArrayList<File>()
        for (dir in dirs) {
            array.addAll(getApkFiles(dir))
        }
        return array
    }

    //获取单个目录下的apk文件
    private fun getApkFiles(dir: File): ArrayList<File> {
        val array = ArrayList<File>()
        for (appDir in dir.listFiles({ pathname: File? ->
            pathname != null && pathname.isDirectory
        })) {
            val apks = appDir.listFiles({ pathname: File? ->
                pathname != null && pathname.extension.toLowerCase() == "apk"
            })
            array.addAll(apks)
        }
        val apks = dir.listFiles({ pathname: File? ->
            pathname != null && pathname.isFile && pathname.extension.toLowerCase() == "apk"
        })
        array.addAll(apks)
        return array
    }

    //按照文件md5值分组文件
    private fun groupByMd5(files: ArrayList<File>): HashMap<String, ArrayList<File>> {
        val hashMap = HashMap<String, ArrayList<File>>()
        for (file in files) {
            val md5 = getMd5ByFile(file)
            if (md5 != null) {
                if (hashMap.containsKey(md5)) {
                    hashMap[md5]!!.add(file)
                } else {
                    hashMap.put(md5, arrayListOf(file))
                }
            }
        }
        return hashMap
    }

    //计算文件的md5值
    private fun getMd5ByFile(file: File): String? {
        val `in` = FileInputStream(file)
        try {
            val byteBuffer = `in`.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(byteBuffer)
            val bi = BigInteger(1, md5.digest())
            return bi.toString(16)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                `in`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    //根据文件在磁盘上的inode分组相同md5的文件
    private fun groupByInode(fileGroups: HashMap<String, ArrayList<File>>): HashMap<String, HashMap<String, ArrayList<File>>> {
        val filterFiles = HashMap<String, HashMap<String, ArrayList<File>>>()
        for (group in fileGroups) {
            val md5 = group.key
            val inodeGroups = HashMap<String, ArrayList<File>>()
            for (file in group.value) {
                val inode = getFileInodeNumber(file)
                if (inodeGroups.containsKey(inode)) {
                    inodeGroups[inode]!!.add(file)
                } else {
                    inodeGroups.put(inode, arrayListOf(file))
                }
            }
            filterFiles.put(md5, inodeGroups)
        }
        return filterFiles
    }

    //获取文件的inode
    private fun getFileInodeNumber(file: File): String {
        return SysUtils.executeCommandWithOutput(false, "ls -li \"${file.absolutePath}\" | cut -f1 -d \" \"").toLowerCase()
    }

    //查找磁盘上inode相同的文件
    private fun findInodeNumber(filename: String, filetype: String = "*.apk"): List<String> {
        return SysUtils.executeCommandWithOutput(false,
                "i=\$(ls -i \"${filename}\" |cut -f1 -d\" \") && find $rootDir -inum \$i -iname \"$filetype\""
        ).trim().split("\n")
    }
}
