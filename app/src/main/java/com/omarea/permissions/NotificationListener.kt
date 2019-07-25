package com.omarea.permissions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.support.v4.app.NotificationManagerCompat

/**
 * 通知监听器 判断是否要拦截
 */
class NotificationListener {
    fun getPermission(context: Context): Boolean {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        if (packageNames.contains(context.getPackageName())) {
            return true;
        }
        return false;
    }

    fun setPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            //intent.data = Uri.parse("package:" + context.getPackageName())
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (ex: Exception) {

        }
    }
}
