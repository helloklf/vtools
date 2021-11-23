package com.omarea.library.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.PermissionChecker
import com.omarea.common.shell.KeepShellPublic

class GeneralPermissions(private val context: Context) {
    private fun checkPermission(permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED

    //启动Activity让用户授权
    // Toast.makeText(context, "Scene未获得显示悬浮窗权限", Toast.LENGTH_SHORT).show()
    // val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
    // context.startActivity(intent);


    fun grantPermissions() {
        val shellStr = StringBuilder()
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
                // 所有文件访问权限（便于内置文件选择器加载文件）
                if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                    shellStr.append("appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow\n")
                }
            } else if (it == Manifest.permission.SYSTEM_ALERT_WINDOW) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(context)) {
                        val option = it.substring("android.permission.".length)
                        shellStr.append("appops set ${context.packageName} ${option} allow\n")
                    }
                } else {
                    if (!checkPermission(it)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val option = it.substring("android.permission.".length)
                            shellStr.append("appops set ${context.packageName} ${option} allow\n")
                        }
                        shellStr.append("pm grant ${context.packageName} $it\n")
                    }
                }
            } else {
                if (!checkPermission(it)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val option = it.substring("android.permission.".length)
                        shellStr.append("appops set ${context.packageName} ${option} allow\n")
                    }
                    shellStr.append("pm grant ${context.packageName} $it\n")
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) {
                shellStr.append("dumpsys deviceidle whitelist +${context.packageName};\n")
            }
        }

        /*
        // 不支持使用ROOT权限进行设置
        if (!checkPermission(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)) {
            cmds.append("pm grant ${context.packageName} android.permission.BIND_NOTIFICATION_LISTENER_SERVICE;\n")
        }
        if (!checkPermission(Manifest.permission.WRITE_SETTINGS)) {
            cmds.append("pm grant ${context.packageName} android.permission.WRITE_SETTINGS;\n")
        }
        */
        KeepShellPublic.doCmdSync(shellStr.toString())
    }
}