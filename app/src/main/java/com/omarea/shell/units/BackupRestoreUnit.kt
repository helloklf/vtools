package com.omarea.shell.units

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.RootFile
import com.omarea.ui.ProgressBarDialog
import java.io.IOException

/**
 * Created by Hello on 2017/11/01.
 */

class BackupRestoreUnit(var context: Context) {
    val dialog: ProgressBarDialog
    internal var myHandler: Handler = Handler()

    companion object {
        fun isSupport(): Boolean {
            return RootFile.itemExists("/dev/block/bootdevice/by-name/boot") || RootFile.itemExists("/dev/block/bootdevice/by-name/recovery")
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
            builder.setPositiveButton(android.R.string.yes, null)
            builder.setMessage(msg + "\n")
            builder.create().show()
        }
    }

    internal fun noRoot() {
        showDialogMsg("请检查ROOT权限", "请检查是否已ROOT手机，并允许本应用访问ROOT权限！")
    }

    internal inner class FlashBootThread(var path: String) : Thread() {

        override fun run() {
            showMsg("即将刷入\n$path\n请勿操作手机！", true)
            showProgressBar()
            try {
                showProgressBar()
                if (KeepShellPublic.doCmdSync("dd if=\"$path\" of=/dev/block/bootdevice/by-name/boot") != "error") {
                    showMsg("操作成功！", true)
                } else {
                    showMsg("镜像刷入失败！", true)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                noRoot()
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                hideProgressBar()
            }
        }
    }

    //刷入Recovery
    fun flashRecovery(path: String) {
        FlashRecoveryThread(path).start()
    }

    internal inner class FlashRecoveryThread(var path: String) : Thread() {

        override fun run() {
            showMsg("即将刷入\n$path\n请勿操作手机！", true)
            showProgressBar()
            try {
                showProgressBar()
                if (KeepShellPublic.doCmdSync("dd if=\"$path\" of=/dev/block/bootdevice/by-name/recovery") != "error") {
                    showMsg("操作成功！", true)
                } else {
                    showMsg("镜像刷入失败", true)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                noRoot()
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                hideProgressBar()
            }
        }
    }


    fun saveBoot() {
        SaveBootThread().start()
    }

    internal inner class SaveBootThread : Thread() {
        override fun run() {
            try {
                showProgressBar()
                if (KeepShellPublic.doCmdSync("dd if=/dev/block/bootdevice/by-name/boot of=${Consts.SDCardDir}/boot.img;\n") != "error") {
                    showMsg("Boot导出成功，保存在${Consts.SDCardDir}/boot.img ！", true)
                } else {
                    showMsg("Boot导出失败！", true)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                noRoot()
            } catch (e: InterruptedException) {
                showMsg("Boot导出失败！", true)
                e.printStackTrace()
            } finally {
                hideProgressBar()
            }
        }
    }


    fun saveRecovery() {
        SaveRecoveryThread().start()
    }

    internal inner class SaveRecoveryThread : Thread() {
        override fun run() {
            showProgressBar()
            if (KeepShellPublic.doCmdSync("dd if=/dev/block/bootdevice/by-name/recovery of=${Consts.SDCardDir}/recovery.img\n") != "error") {
                showMsg("Recovery导出成功，已保存为${Consts.SDCardDir}/recovery.img ！", true)
            } else {
                showMsg("Recovery导出失败！", true)
            }
            hideProgressBar()
        }
    }

}
