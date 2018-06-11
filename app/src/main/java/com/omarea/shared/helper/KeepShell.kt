package com.omarea.shared.helper

import android.content.Context
import android.os.Build
import android.os.Handler
import android.widget.Toast
import java.io.BufferedWriter
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Created by Hello on 2018/01/23.
 */

class KeepShell(private var context: Context?) {
    private var p: Process? = null
    private var out: BufferedWriter? = null
    private var handler: Handler = Handler()

    fun setContext(context: Context?) {
        this.context = context
    }

    private fun showMsg(msg: String) {
        if (context != null)
            handler.post {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
    }

    //尝试退出命令行程序
    internal fun tryExit() {
        try {
            if (out != null)
                out!!.close()
            out = null
        } catch (ex: Exception) {
        }
        out = null
        try {
            p!!.destroy()
        } catch (ex: Exception) {
        }
        p = null
    }

    //获取ROOT超时时间
    private val GET_ROOT_TIMEOUT = 10000L

    private fun getRuntimeShell(cmd: String?, error: Runnable?) {
        val thread = Thread(Runnable {
            try {
                tryExit()
                p = Runtime.getRuntime().exec("su")
                out = p!!.outputStream.bufferedWriter()
                if (out == null) {
                    error?.run()
                } else if (cmd != null) {
                    out!!.write(cmd)
                    out!!.write("\n\n")
                    out!!.flush()
                }
            } catch (e: Exception) {
                if (out == null) {
                    error?.run()
                } else {
                    showMsg("获取ROOT权限失败！")
                }
            }
        })
        thread.start()
        handler.postDelayed({
            if (p == null && thread.isAlive) {
                thread.interrupt()
                tryExit()
                if (error != null) {
                    error.run()
                } else {
                    showMsg("获取Root权限超时！")
                }
            }
        }, GET_ROOT_TIMEOUT)
    }

    //执行脚本
    internal fun doCmd(cmd: String, isRedo: Boolean = false) {
        try {
            //tryExit()
            if (p == null || isRedo || out == null) {
                getRuntimeShell(cmd, Runnable {
                    //重试一次
                    if (!isRedo)
                        doCmd(cmd, true)
                    else
                        showMsg("Failed execution action!\nError message : Unable to obtain Root permissions\n\n\ncommand : \r\n$cmd")
                })
            } else {
                out!!.write(cmd)
                out!!.write("\n\n")
                out!!.flush()
            }
        } catch (e: IOException) {
            //重试一次
            if (!isRedo)
                doCmd(cmd, true)
            else
                showMsg("Failed execution action!\nError message : " + e.message + "\n\n\ncommand : \r\n" + cmd)
        }
    }
}
