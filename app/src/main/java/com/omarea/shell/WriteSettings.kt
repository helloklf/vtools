package com.omarea.shell

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.support.v4.content.PermissionChecker

/**
 * Created by SYSTEM on 2018/07/21.
 */

class WriteSettings {
    private fun checkPermission(context: Context, permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
    fun getPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            // TODO("VERSION.SDK_INT < M")
            return true
        }
    }

    fun setPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val selfPackageUri = Uri.parse("package:" + context.packageName)
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, selfPackageUri)
            // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            KeepShellSync.doCmdSync("pm grant com.omarea.vtools android.permission.WRITE_SETTINGS")
        }
    }
}
