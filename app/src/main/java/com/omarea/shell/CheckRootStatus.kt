package com.omarea.shell

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import com.omarea.ui.ProgressBarDialog
import com.omarea.vboot.R

/**
 * 检查获取root权限
 * Created by helloklf on 2017/6/3.
 */

class CheckRootStatus(var context: Context, private var next:Runnable? = null, private var skip:Runnable?) {
    var myHandler: Handler = Handler()

    //是否已经Root
    private fun isRoot(): Boolean {
        var process: java.lang.Process? = null
        try {
            process = Runtime.getRuntime().exec("su")
            val out = process!!.outputStream.bufferedWriter()
            out.write("setenforce 0;\n")
            out.write("dumpsys deviceidle whitelist +com.omarea.vboot;\n")
            out.write("exit;\n")
            out.write("exit;\n")
            out.flush()

            process.waitFor()
            val r = process.exitValue() == 0
            process.destroy()
            return r
            //if (msg == "permission denied" || msg.contains("not allowed") || msg == "not found")
        } catch (e: Exception) {
            if (process != null)
                process.destroy()
            e.stackTrace
            return false
        }

    }

    fun forceGetRoot() {
        val pd = ProgressBarDialog(context)
        pd.showDialog("正在检查ROOT权限")
        var completed = false
        Thread {
            if (!isRoot()) {
                completed = true
                myHandler.post {
                    pd.hideDialog()
                    val alert = AlertDialog.Builder(context)
                    alert.setCancelable(false)
                    alert.setTitle(R.string.error_root)
                    alert.setNegativeButton(R.string.btn_refresh, { _, _ ->
                        forceGetRoot()
                    })
                    alert.setNeutralButton(R.string.btn_skip, { _, _ ->
                        //android.os.Process.killProcess(android.os.Process.myPid())
                        completed = true
                        myHandler.post {
                            pd.hideDialog()
                            if (skip != null)
                                skip!!.run()
                        }
                    })
                    alert.create().show()
                }
            } else {
                completed = true
                myHandler.post {
                    pd.hideDialog()
                    if (next != null)
                        next!!.run()
                }
            }
        }.start()
        myHandler.postDelayed({
            if (!completed) {
                pd.hideDialog()
                val alert = AlertDialog.Builder(context)
                alert.setCancelable(false)
                alert.setTitle(R.string.error_root)
                alert.setMessage(R.string.error_su_timeout)
                alert.setNegativeButton(R.string.btn_refresh, { _, _ ->
                    forceGetRoot()
                })
                alert.setNeutralButton(R.string.btn_exit, { _, _ ->
                    android.os.Process.killProcess(android.os.Process.myPid())
                })
                alert.create().show()
            }
        }, 10000)
    }
}
