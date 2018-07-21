package com.omarea.shell

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.support.annotation.RequiresApi

/**
 * Created by SYSTEM on 2018/07/21.
 */

class WriteSettings {
    fun setPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:" + context.getPackageName())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
            }
        } else {
            KeepShellSync.doCmdSync("pm grant com.omarea.vtools android.permission.WRITE_SETTINGS")
        }
    }
}
