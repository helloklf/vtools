package com.omarea.shell

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import com.omarea.vboot.R


/**
 * Created by helloklf on 2017/6/3.
 */

class CheckRootStatus(var progressBar: ProgressBar, var context: Context, var next:Runnable? = null) {
    var myHandler: Handler = Handler()

    //是否已经Root
    fun isRoot(): Boolean {
        var process: java.lang.Process? = null
        try {
            process = Runtime.getRuntime().exec("su")
            val out = process!!.outputStream.bufferedWriter()
            out.write("setenforce 0;\ndumpsys deviceidle whitelist +com.omarea.vboot;\nexit;\nexit;\n")
            out.flush()

            var msg = ""

            process.waitFor()
            val r = process.exitValue() == 0
            process.destroy()
            return r
            //if (msg == "permission denied" || msg.contains("not allowed") || msg == "not found")
        } catch (e: Exception) {
            if (process != null)
                process.destroy()
            return false
        }

    }

    fun forceGetRoot() {
        progressBar.visibility = View.VISIBLE
        var completed = false
        Thread {
            if (!isRoot()) {
                completed = true
                myHandler.post {
                    progressBar.visibility = View.GONE
                    val alert = AlertDialog.Builder(context)
                    alert.setCancelable(false)
                    alert.setTitle(R.string.error_root)
                    alert.setNegativeButton(R.string.btn_refresh, { _, _ ->
                        forceGetRoot()
                    })
                    alert.setNeutralButton(R.string.btn_exit, { _, _ ->
                        android.os.Process.killProcess(android.os.Process.myPid())
                    })
                    alert.create().show()
                }
            } else {
                completed = true
                myHandler.post {
                    progressBar.visibility = View.GONE
                    if (next != null)
                        next!!.run()
                }
            }
        }.start()
        myHandler.postDelayed({
            if (!completed) {
                progressBar.visibility = View.GONE
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
