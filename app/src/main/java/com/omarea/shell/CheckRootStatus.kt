package com.omarea.shell

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v4.content.PermissionChecker
import android.util.Log
import com.omarea.shared.Consts
import com.omarea.ui.ProgressBarDialog
import com.omarea.vtools.R

/**
 * 检查获取root权限
 * Created by helloklf on 2017/6/3.
 */

class CheckRootStatus(var context: Context, private var next: Runnable? = null, private var skip: Runnable?, private var disableSeLinux: Boolean = false) {
    var myHandler: Handler = Handler(Looper.getMainLooper())

    //是否已经Root
    private fun isRoot(disableSeLinux: Boolean): Boolean {
        val r = KeepShellSync.doCmdSync(Consts.isRootUser)
        Log.d("getsu", r)
        if (r == "error" || r.contains("permission denied") || r.contains("not allowed") || r.equals("not found")) {
            return false
        } else if (r == "root") {
            if (disableSeLinux)
                KeepShellSync.doCmdSync(Consts.DisableSELinux)
            return true
        } else {
            return false
        }
    }


    var therad: Thread? = null
    fun forceGetRoot() {
        val pd = ProgressBarDialog(context)
        pd.showDialog("正在检查ROOT权限")
        var completed = false
        therad = Thread {
            if (!isRoot(disableSeLinux)) {
                completed = true
                myHandler.post {
                    pd.hideDialog()
                    val alert = AlertDialog.Builder(context)
                    alert.setCancelable(false)
                    alert.setTitle(R.string.error_root)
                    alert.setNegativeButton(R.string.btn_refresh, { _, _ ->
                        if (therad != null && therad!!.isAlive && !therad!!.isInterrupted) {
                            therad!!.interrupt()
                            therad = null
                        }
                        forceGetRoot()
                    })
                    alert.setNeutralButton(R.string.btn_skip, { _, _ ->
                        //android.os.Process.killProcess(android.os.Process.myPid())
                        completed = true
                        if (therad != null && therad!!.isAlive && !therad!!.isInterrupted) {
                            therad!!.interrupt()
                            therad = null
                        }
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
        };
        therad!!.start()
        myHandler.postDelayed({
            if (!completed) {
                pd.hideDialog()
                val alert = AlertDialog.Builder(context)
                alert.setCancelable(false)
                alert.setTitle(R.string.error_root)
                alert.setMessage(R.string.error_su_timeout)
                alert.setNegativeButton(R.string.btn_refresh, { _, _ ->
                    if (therad != null && therad!!.isAlive && !therad!!.isInterrupted) {
                        therad!!.interrupt()
                        therad = null
                    }
                    forceGetRoot()
                })
                alert.setNeutralButton(R.string.btn_skip, { _, _ ->
                    if (therad != null && therad!!.isAlive && !therad!!.isInterrupted) {
                        therad!!.interrupt()
                        therad = null
                    }
                    completed = true
                    myHandler.post {
                        pd.hideDialog()
                        if (skip != null)
                            skip!!.run()
                    }
                    //android.os.Process.killProcess(android.os.Process.myPid())
                })
                alert.create().show()
            }
        }, 15000)
    }

    companion object {
        private fun checkPermission(context: Context, permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
        fun grantPermission(context: Context) {
            val cmds = StringBuilder()
            cmds.append("dumpsys deviceidle whitelist +com.omarea.vtools;\n")
            if (!checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                cmds.append("pm grant com.omarea.vtools android.permission.READ_EXTERNAL_STORAGE;\n")
            }
            if (!checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                cmds.append("pm grant com.omarea.vtools android.permission.WRITE_EXTERNAL_STORAGE;\n")
            }
            if (!checkPermission(context, Manifest.permission.CHANGE_CONFIGURATION)) {
                cmds.append("pm grant com.omarea.vtools android.permission.CHANGE_CONFIGURATION;\n")
            }
            if (!checkPermission(context, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)) {
                cmds.append("pm grant com.omarea.vtools android.permission.BIND_NOTIFICATION_LISTENER_SERVICE;\n")
            }
            if (!checkPermission(context, Manifest.permission.WRITE_SETTINGS)) {
                cmds.append("pm grant com.omarea.vtools android.permission.WRITE_SETTINGS;\n")
            }
            KeepShellSync.doCmdSync(cmds.toString())
        }

        public fun isMagisk(): Boolean {
            return SysUtils.executeCommandWithOutput(false, "su -v").contains("MAGISKSU")
        }

        public fun isTmpfs(dir: String): Boolean {
            return SysUtils.executeCommandWithOutput(false, " df | grep tmpfs | grep \"$dir\"").trim().length > 0
        }
    }
}
