package com.omarea.shell.units

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shell.SuDo
import com.omarea.ui.ProgressBarDialog
import java.io.DataOutputStream
import java.io.IOException

/**
 * Created by Hello on 2017/11/01.
 */

class BackupRestoreUnit(var context: Context) {
    val dialog: ProgressBarDialog
    internal var myHandler: Handler = Handler()

    init {
        dialog = ProgressBarDialog(context)
    }

    //显示进度条
    fun ShowProgressBar() {
        myHandler.post {
            dialog.showDialog("正在执行操作...")
        }
    }

    //隐藏进度条
    fun HideProgressBar() {
        myHandler.post {
            dialog.hideDialog()
        }
    }

    //显示文本消息
    fun ShowMsg(msg: String, longMsg: Boolean) {
        if (context != null)
            myHandler.post { Toast.makeText(context, msg, if (longMsg) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() }
    }

    //刷入Boot
    fun FlashBoot(path: String) {
        FlashBootThread(path).start()
    }

    //显示弹窗提示
    fun ShowDialogMsg(title: String, msg: String) {
        myHandler.post {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setPositiveButton(android.R.string.yes, null)
            builder.setMessage(msg + "\n")
            builder.create().show()
        }
    }

    internal fun NoRoot() {
        ShowDialogMsg("请检查ROOT权限", "请检查是否已ROOT手机，并允许本应用访问ROOT权限！")
    }

    internal inner class FlashBootThread(var path: String) : Thread() {

        override fun run() {
            ShowMsg("即将刷入\n$path\n请勿操作手机！", true)
            ShowProgressBar()
            try {
                ShowProgressBar()
                if (SuDo(context).execCmdSync("dd if=$path of=/dev/block/bootdevice/by-name/boot")) {
                    ShowMsg("操作成功！", true)
                } else {
                    ShowMsg("镜像刷入失败！", true)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                NoRoot()
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                HideProgressBar()
            }
        }
    }

    //刷入Recovery
    fun FlashRecovery(path: String) {
        FlashRecoveryThread(path).start()
    }

    internal inner class FlashRecoveryThread(var path: String) : Thread() {

        override fun run() {
            ShowMsg("即将刷入\n$path\n请勿操作手机！", true)
            ShowProgressBar()
            try {
                ShowProgressBar()
                if (SuDo(context).execCmdSync("dd if=$path of=/dev/block/bootdevice/by-name/recovery")) {
                    ShowMsg("操作成功！", true)
                } else {
                    ShowMsg("镜像刷入失败", true)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                NoRoot()
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                HideProgressBar()
            }
        }
    }


    fun SaveBoot() {
        SaveBootThread().start()
    }

    internal inner class SaveBootThread : Thread() {
        override fun run() {
            try {
                ShowProgressBar()
                if (SuDo(context).execCmdSync("dd if=/dev/block/bootdevice/by-name/boot of=${Consts.SDCardDir}/boot.img;\n")) {
                    ShowMsg("Boot导出失败！", true)
                } else {
                    ShowMsg("Boot导出成功，保存在${Consts.SDCardDir}/boot.img ！", true)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                NoRoot()
            } catch (e: InterruptedException) {
                ShowMsg("Boot导出失败！", true)
                e.printStackTrace()
            } finally {
                HideProgressBar()
            }
        }
    }


    fun SaveRecovery() {
        SaveRecoveryThread().start()
    }

    internal inner class SaveRecoveryThread : Thread() {
        override fun run() {
            ShowProgressBar()
            if (SuDo(context).execCmdSync("dd if=/dev/block/bootdevice/by-name/recovery of=${Consts.SDCardDir}/recovery.img\n")!!) {
                ShowMsg("Recovery导出成功，已保存为${Consts.SDCardDir}/recovery.img ！", true)
            } else {
                ShowMsg("Recovery导出失败！", true)
            }
            HideProgressBar()
        }
    }

}
