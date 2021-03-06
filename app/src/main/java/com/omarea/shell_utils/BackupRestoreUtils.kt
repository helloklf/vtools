package com.omarea.shell_utils

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.utils.CommonCmds

/**
 * Created by Hello on 2017/11/01.
 */

class BackupRestoreUtils(var context: Activity) {
    val dialog: ProgressBarDialog = ProgressBarDialog(context)
    internal var myHandler: Handler = Handler(Looper.getMainLooper())

    companion object {
        private var bootPartPath = "/dev/block/bootdevice/by-name/boot"
        private var recPartPath = "/dev/block/bootdevice/by-name/recovery"
        private var dtboPartPath = "/dev/block/bootdevice/by-name/dtbo"
        private var persistPartPath = "/dev/block/bootdevice/by-name/persist"

        fun isSupport(): Boolean {
            if (RootFile.itemExists(bootPartPath) || RootFile.itemExists(recPartPath)) {
                return true
            }

            var r = false
            val boots = KeepShellPublic.doCmdSync("ls /dev/block/platform/*/by-name/BOOT").split("\n")
            if (boots.isNotEmpty() && boots.first().startsWith("/dev/block/platform") && boots.first().endsWith("/by-name/BOOT")) {
                bootPartPath = boots.first()
                r = true
            }
            val recs = KeepShellPublic.doCmdSync("ls /dev/block/platform/*/by-name/RECOVERY").split("\n")
            if (recs.isNotEmpty() && recs.first().startsWith("/dev/block/platform") && recs.first().endsWith("/by-name/RECOVERY")) {
                recPartPath = recs.first()
                r = true
            }
            val dtbos = KeepShellPublic.doCmdSync("ls /dev/block/platform/*/by-name/DTBO").split("\n")
            if (dtbos.isNotEmpty() && dtbos.first().startsWith("/dev/block/platform") && dtbos.first().endsWith("/by-name/DTBO")) {
                dtboPartPath = dtbos.first()
                r = true
            }
            val persists = KeepShellPublic.doCmdSync("ls /dev/block/platform/*/by-name/PERSIST").split("\n")
            if (persists.isNotEmpty() && persists.first().startsWith("/dev/block/platform") && persists.first().endsWith("/by-name/PERSIST")) {
                persistPartPath = persists.first()
                r = true
            }
            return r
        }
    }

    //显示进度条
    fun showProgressBar() {
        myHandler.post {
            dialog.showDialog("正在执行操作...")
        }
    }

    //隐藏进度条
    fun hideProgressBar() {
        myHandler.post {
            dialog.hideDialog()
        }
    }

    //显示文本消息
    fun showMsg(msg: String, longMsg: Boolean) {
        myHandler.post {
            Toast.makeText(context, msg, if (longMsg) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    //刷入Boot
    fun flashBoot(path: String) {
        FlashImgThread(path, bootPartPath).start()
    }

    fun flashRecovery(path: String) {
        FlashImgThread(path, recPartPath).start()
    }

    fun flashDTBO(path: String) {
        FlashImgThread(path, dtboPartPath).start()
    }

    fun flashPersist(path: String) {
        FlashImgThread(path, persistPartPath).start()
    }

    internal inner class FlashImgThread(var inputPath: String, var outputPath: String) : Thread() {
        override fun run() {
            if (!isSupport() || !RootFile.itemExists(outputPath)) {
                showMsg("暂不支持您的设备！", true)
                return
            }
            showMsg("即将刷入\n$inputPath\n请勿操作手机！", true)
            showProgressBar()
            if (KeepShellPublic.doCmdSync("dd if=\"$inputPath\" of=$outputPath") != "error") {
                showMsg("操作成功！", true)
            } else {
                showMsg("镜像刷入失败", true)
            }
            hideProgressBar()
        }
    }


    fun saveBoot() {
        SaveImgThread(bootPartPath, "boot").start()
    }

    fun saveRecovery() {
        SaveImgThread(recPartPath, "recovery").start()
    }

    fun saveDTBO() {
        SaveImgThread(dtboPartPath, "dtbo").start()
    }

    fun savePersist() {
        SaveImgThread(persistPartPath, "persist").start()
    }

    internal inner class SaveImgThread(var inputPath: String, var outputName: String) : Thread() {
        override fun run() {
            val outPath = "${CommonCmds.SDCardDir}/$outputName.img"

            if (!isSupport() || !RootFile.itemExists(inputPath)) {
                showMsg("暂不支持您的设备！", true)
                return
            }
            showProgressBar()
            if (KeepShellPublic.doCmdSync("dd if=$inputPath of=$outPath\n") != "error") {
                showMsg("分区镜像导出成功，已保存为$outPath ！", true)
            } else {
                showMsg("分区镜像导出失败！", true)
            }
            hideProgressBar()
        }
    }
}
