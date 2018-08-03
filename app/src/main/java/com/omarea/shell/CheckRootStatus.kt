package com.omarea.shell

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v4.content.PermissionChecker
import android.util.Log
import com.omarea.shared.CommonCmds
import com.omarea.vtools.R

/**
 * 检查获取root权限
 * Created by helloklf on 2017/6/3.
 */

class CheckRootStatus(var context: Context, private var next: Runnable? = null, private var skip: Runnable?, private var disableSeLinux: Boolean = false) {
    var myHandler: Handler = Handler(Looper.getMainLooper())
    val isRootUser = "if [[ `id -u 2>&1` = '0' ]]; then\n" +
            "\techo 'root';\n" +
            "elif [[ `\$UID` = '0' ]]; then\n" +
            "\techo 'root';\n" +
            "elif [[ `whoami 2>&1` = 'root' ]]; then\n" +
            "\techo 'root';\n" +
            "elif [[ `set | grep 'USER_ID=0'` = 'USER_ID=0' ]]; then\n" +
            "\techo 'root';\n" +
            "else\n" +
            "\texit -1;\n" +
            "fi;"
    //是否已经Root
    private fun isRoot(disableSeLinux: Boolean): Boolean {
        val r = KeepShellPublic.doCmdSync(isRootUser)
        Log.d("getsu", r)
        if (r == "error" || r.contains("permission denied") || r.contains("not allowed") || r.equals("not found")) {
            return false
        } else if (r == "root") {
            if (disableSeLinux)
                KeepShellPublic.doCmdSync(CommonCmds.DisableSELinux)
            return true
        } else {
            return false
        }
    }


    var therad: Thread? = null
    fun forceGetRoot() {
        var completed = false
        therad = Thread {
            if (!isRoot(disableSeLinux)) {
                completed = true
                myHandler.post {
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
                            if (skip != null)
                                skip!!.run()
                        }
                    })
                    alert.create().show()
                }
            } else {
                completed = true
                myHandler.post {
                    if (next != null)
                        next!!.run()
                }
            }
        };
        therad!!.start()
        myHandler.postDelayed({
            if (!completed) {
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
            cmds.append("dumpsys deviceidle whitelist +${context.packageName};\n")
            // 必需的权限
            val requiredPermission = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CHANGE_CONFIGURATION,
                    Manifest.permission.WRITE_SECURE_SETTINGS,
                    Manifest.permission.SYSTEM_ALERT_WINDOW
            )
            requiredPermission.forEach {
                if (!checkPermission(context, it)) {
                    cmds.append("pm grant ${context.packageName} $it;\n")
                }
            }

            /*
            // 不支持使用ROOT权限进行设置
            if (!checkPermission(context, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)) {
                cmds.append("pm grant ${context.packageName} android.permission.BIND_NOTIFICATION_LISTENER_SERVICE;\n")
            }
            if (!checkPermission(context, Manifest.permission.WRITE_SETTINGS)) {
                cmds.append("pm grant ${context.packageName} android.permission.WRITE_SETTINGS;\n")
            }
            */
            KeepShellPublic.doCmdSync(cmds.toString())
        }

        public fun isMagisk(): Boolean {
            return SysUtils.executeCommandWithOutput(false, "su -v").contains("MAGISKSU")
        }

        public fun isTmpfs(dir: String): Boolean {
            return SysUtils.executeCommandWithOutput(false, " df | grep tmpfs | grep \"$dir\"").trim().isNotEmpty()
        }
    }
}
