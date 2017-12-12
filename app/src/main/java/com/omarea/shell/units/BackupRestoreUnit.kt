package com.omarea.shell.units

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.omarea.shared.Consts

import com.omarea.shared.ShellRuntime
import com.omarea.shared.cmd_shellTools

import java.io.DataOutputStream
import java.io.IOException

/**
 * Created by Hello on 2017/11/01.
 */

class BackupRestoreUnit(var activity: Activity?, var progressBar: ProgressBar?) {
    var context: Context? = null

    internal var myHandler: Handler = Handler()

    init {
        if (activity != null)
            this.context = activity!!.applicationContext
    }

    //显示进度条
    fun ShowProgressBar() {
        myHandler.post {
            if (progressBar != null)
                progressBar!!.visibility = View.VISIBLE
        }
    }

    //隐藏进度条
    fun HideProgressBar() {
        myHandler.post {
            if (progressBar != null)
                progressBar!!.visibility = View.GONE
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
        if (activity != null)
            myHandler.post {
                val builder = AlertDialog.Builder(activity)
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
                val p = Runtime.getRuntime().exec("su")
                val dos = DataOutputStream(p.outputStream)
                dos.writeChars("dd if=$path of=/dev/block/bootdevice/by-name/boot\n")
                dos.writeChars("exit\n")
                dos.writeChars("exit\n")
                dos.flush()
                if (p.waitFor() != 0) {
                    ShowMsg("镜像刷入失败！", true)
                } else {
                    ShowMsg("操作成功！", true)
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
                val p = Runtime.getRuntime().exec("su")
                val dos = DataOutputStream(p.outputStream)
                dos.writeChars("dd if=$path of=/dev/block/bootdevice/by-name/recovery\n")
                dos.writeChars("exit\n")
                dos.writeChars("exit\n")
                dos.flush()
                if (p.waitFor() != 0) {
                    ShowMsg("镜像刷入失败", true)
                } else {
                    ShowMsg("操作成功！", true)
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
                val p = Runtime.getRuntime().exec("su")
                val dataOutputStream = DataOutputStream(p.outputStream)
                dataOutputStream.writeBytes("dd if=/dev/block/bootdevice/by-name/boot of=${Consts.SDCardDir}/boot.img;\n")
                dataOutputStream.writeBytes("exit\n")
                dataOutputStream.writeBytes("exit\n")
                dataOutputStream.flush()
                if (p.waitFor() == 0) {
                    ShowMsg("Boot导出成功，保存在${Consts.SDCardDir}/boot.img ！", true)
                } else {
                    ShowMsg("Boot导出失败！", true)
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
            if (ShellRuntime().execute("dd if=/dev/block/bootdevice/by-name/recovery of=${Consts.SDCardDir}/recovery.img\n")!!) {
                ShowMsg("Recovery导出成功，已保存为${Consts.SDCardDir}/recovery.img ！", true)
            } else {
                ShowMsg("Recovery导出失败！", true)
            }
            HideProgressBar()
        }
    }

}
