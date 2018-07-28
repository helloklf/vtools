package com.omarea.shell

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.support.v4.content.PermissionChecker
import android.widget.Toast


/**
 * Created by SYSTEM on 2018/07/21.
 */

class WriteSettings {
    private fun checkPermission(context: Context, permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
    fun getPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(context)
        } else {
            // TODO("VERSION.SDK_INT < M")
            return true
        }
    }

    fun setPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // val selfPackageUri = Uri.parse("package:" + context.packageName)
            // val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, selfPackageUri)
            // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // context.startActivity(intent)
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.getPackageName(), null)
            intent.setData(uri)
            context.startActivity(intent)
            Toast.makeText(context, "请为Scene授予“修改系统设置”权限！", Toast.LENGTH_SHORT).show()
        } else {
            KeepShellSync.doCmdSync("pm grant ${context.packageName} android.permission.WRITE_SETTINGS")
        }
    }
}
