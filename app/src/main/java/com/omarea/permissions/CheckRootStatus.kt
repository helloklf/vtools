package com.omarea.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.PermissionChecker
import com.omarea.Scene
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * 检查获取root权限
 * Created by helloklf on 2017/6/3.
 */

public class CheckRootStatus(var context: Context, private val next: Runnable? = null, private var disableSeLinux: Boolean = false, private val skip: Runnable? = null) {
    var myHandler: Handler = Handler(Looper.getMainLooper())

    var therad: Thread? = null
    public fun forceGetRoot() {
        if (lastCheckResult) {
            if (next != null) {
                myHandler.post(next)
            }
        } else {
            var completed = false
            therad = Thread {
                setRootStatus(KeepShellPublic.checkRoot())

                if (completed) {
                    return@Thread
                }

                completed = true

                if (lastCheckResult) {
                    if (disableSeLinux) {
                        KeepShellPublic.doCmdSync(CommonCmds.DisableSELinux)
                    }
                    if (next != null) {
                        myHandler.post(next)
                    }
                } else {
                    myHandler.post {
                        KeepShellPublic.tryExit()

                        val view = LayoutInflater.from(context).inflate(R.layout.dialog_root_rejected, null)
                        DialogHelper.customDialog(context, view, false).apply {
                            view.findViewById<View>(R.id.btn_retry).setOnClickListener {
                                dismiss()

                                KeepShellPublic.tryExit()
                                if (therad != null && therad!!.isAlive && !therad!!.isInterrupted) {
                                    therad!!.interrupt()
                                    therad = null
                                }
                                forceGetRoot()
                            }
                            view.findViewById<View>(R.id.btn_skip).setOnClickListener {
                                dismiss()
                                skip?.run {
                                    myHandler.post(skip)
                                }
                                //android.os.Process.killProcess(android.os.Process.myPid())
                            }
                        }

                    }
                }
            }
            therad!!.start()
            Thread {
                Thread.sleep(1000 * 15)

                if (!completed) {
                    KeepShellPublic.tryExit()
                    myHandler.post {
                        val view = LayoutInflater.from(context).inflate(R.layout.dialog_root_timeout, null)
                        DialogHelper.customDialog(context, view, false).apply {
                            view.findViewById<View>(R.id.btn_retry).setOnClickListener {
                                if (therad != null && therad!!.isAlive && !therad!!.isInterrupted) {
                                    therad!!.interrupt()
                                    therad = null
                                }
                                forceGetRoot()
                            }
                            view.findViewById<View>(R.id.btn_exit).setOnClickListener {
                                dismiss()

                                exitProcess(0)
                                //android.os.Process.killProcess(android.os.Process.myPid())
                            }
                        }
                    }
                }
            }.start()
        }
    }

    companion object {
        private var rootStatus = false

        public fun checkRootAsync() {
            GlobalScope.launch(Dispatchers.IO) {
                setRootStatus(KeepShellPublic.checkRoot())
            }
        }

        private fun checkPermission(context: Context, permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED

        fun grantPermission(context: Context) {
            val cmds = StringBuilder()
            // 必需的权限
            val requiredPermission = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CHANGE_CONFIGURATION,
                    Manifest.permission.WRITE_SECURE_SETTINGS,
                    Manifest.permission.SYSTEM_ALERT_WINDOW,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    // Manifest.permission.UNINSTALL_SHORTCUT,
                    // Manifest.permission.INSTALL_SHORTCUT
            )
            requiredPermission.forEach {
                if (it == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
                    if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                        cmds.append("appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow\n")
                    }
                } else if (it == Manifest.permission.SYSTEM_ALERT_WINDOW) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!Settings.canDrawOverlays(context)) {
                            // 未允许悬浮窗
                            try {
                                //启动Activity让用户授权
                                // Toast.makeText(context, "Scene未获得显示悬浮窗权限", Toast.LENGTH_SHORT).show()
                                // val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                                // context.startActivity(intent);
                            } catch (ex: Exception) {
                            }
                        }
                    } else {
                        if (!checkPermission(context, it)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val option = it.substring("android.permission.".length)
                                cmds.append("appops set ${context.packageName} ${option} allow\n")
                            }
                            cmds.append("pm grant ${context.packageName} $it\n")
                        }
                    }
                } else {
                    if (!checkPermission(context, it)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val option = it.substring("android.permission.".length)
                            cmds.append("appops set ${context.packageName} ${option} allow\n")
                        }
                        cmds.append("pm grant ${context.packageName} $it\n")
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!checkPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) {
                    cmds.append("dumpsys deviceidle whitelist +${context.packageName};\n")
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

        // 最后的ROOT检测结果
        val lastCheckResult: Boolean
            get() {
                return rootStatus
            }

        private fun setRootStatus(root: Boolean) {
            rootStatus = root
            Scene.setBoolean("root", root)
        }
    }
}
