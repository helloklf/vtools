package com.omarea.shell_utils

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.utils.CommonCmds

/**
 * Created by Hello on 2017/11/01.
 */

class BackupRestoreUtils(var context: Context) {
    val dialog: ProgressBarDialog
    internal var myHandler: Handler = Handler()

    companion object {
        private var bootPartPath = "/dev/block/bootdevice/by-name/boot"
        private var recPartPath = "/dev/block/bootdevice/by-name/recovery"
        fun isSupport(): Boolean {
            if (RootFile.itemExists(bootPartPath) || RootFile.itemExists(recPartPath)) {
                return true
            }

            var r = false
            val boots = KeepShellPublic.doCmdSync("ls /dev/block/platform/*/by-name/BOOT").split("\n")
            if (boots.size > 0 && boots.first().startsWith("/dev/block/platform") && boots.first().endsWith("/by-name/BOOT")) {
                bootPartPath = boots.first()
                r = true
            }
            val recs = KeepShellPublic.doCmdSync("ls /dev/block/platform/*/by-name/RECOVERY").split("\n")
            if (recs.size > 0 && recs.first().startsWith("/dev/block/platform") && recs.first().endsWith("/by-name/RECOVERY")) {
                recPartPath = recs.first()
                r = true
            }
            return r
        }
    }

    init {
        dialog = ProgressBarDialog(context)
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
        FlashBootThread(path).start()
    }

    //显示弹窗提示
    fun showDialogMsg(title: String, msg: String) {
        myHandler.post {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setPositiveButton(android.R.string.yes, { _, _ ->
            })
            builder.setMessage(msg + "\n")
            builder.create().show()
        }
    }

    internal fun noRoot() {
        showDialogMsg("请检查ROOT权限", "请检查是否已ROOT手机，并允许本应用访问ROOT权限！")
    }

    internal inner class FlashBootThread(var path: String) : Thread() {
        override fun run() {
            if (!isSupport() || !RootFile.itemExists(bootPartPath)) {
                showMsg("暂不支持您的设备！", true)
                return
            }
            showMsg("即将刷入\n$path\n请勿操作手机！", true)
            showProgressBar()
            if (KeepShellPublic.doCmdSync("dd if=\"$path\" of=$bootPartPath") != "error") {
                showMsg("操作成功！", true)
            } else {
                showMsg("镜像刷入失败！", true)
            }
            hideProgressBar()
        }
    }

    //刷入Recovery
    fun flashRecovery(path: String) {
        FlashRecoveryThread(path).start()
    }

    internal inner class FlashRecoveryThread(var path: String) : Thread() {
        override fun run() {
            if (!isSupport() || !RootFile.itemExists(recPartPath)) {
                showMsg("暂不支持您的设备！", true)
                return
            }
            showMsg("即将刷入\n$path\n请勿操作手机！", true)
            showProgressBar()
            if (KeepShellPublic.doCmdSync("dd if=\"$path\" of=$recPartPath") != "error") {
                showMsg("操作成功！", true)
            } else {
                showMsg("镜像刷入失败", true)
            }
            hideProgressBar()
        }
    }


    fun saveBoot() {
        SaveBootThread().start()
    }

    internal inner class SaveBootThread : Thread() {
        override fun run() {
            if (!isSupport() || !RootFile.itemExists(bootPartPath)) {
                showMsg("暂不支持您的设备！", true)
                return
            }
            showProgressBar()
            if (KeepShellPublic.doCmdSync("dd if=$bootPartPath of=${CommonCmds.SDCardDir}/boot.img;\n") != "error") {
                showMsg("Boot导出成功，保存在${CommonCmds.SDCardDir}/boot.img ！", true)
            } else {
                showMsg("Boot导出失败！", true)
            }
            hideProgressBar()
        }
    }


    fun saveRecovery() {
        SaveRecoveryThread().start()
    }

    internal inner class SaveRecoveryThread : Thread() {
        override fun run() {
            if (!isSupport() || !RootFile.itemExists(recPartPath)) {
                showMsg("暂不支持您的设备！", true)
                return
            }
            showProgressBar()
            if (KeepShellPublic.doCmdSync("dd if=$recPartPath of=${CommonCmds.SDCardDir}/recovery.img\n") != "error") {
                showMsg("Recovery导出成功，已保存为${CommonCmds.SDCardDir}/recovery.img ！", true)
            } else {
                showMsg("Recovery导出失败！", true)
            }
            hideProgressBar()
        }
    }

}
