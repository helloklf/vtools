package com.omarea.shared.helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import com.omarea.vboot.ActivityMain
import com.omarea.vboot.ActivityQuickSwitchMode
import com.omarea.vboot.R


/**
 * 用于在通知栏显示通知
 * Created by Hello on 2018/01/23.
 */

internal class NotifyHelper(private var context: Context, notify: Boolean = false) {
    private var showNofity:Boolean = false
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null

    private fun getModName(mode:String) : String {
        when(mode) {
            "powersave" ->      return "省电模式"
            "performance" ->      return "性能模式"
            "fast" ->      return "极速模式"
            "balance" ->   return "均衡模式"
            else ->         return "未知模式"
        }
    }

    private fun getAppName(packageName: String): CharSequence? {
        try {
            return context.packageManager.getPackageInfo(packageName, 0).applicationInfo.loadLabel(context.packageManager)
        } catch (ex: Exception) {
            return packageName
        }
    }

    private fun getModIcon(mode: String): Int {
        when(mode) {
            "powersave" ->      return R.drawable.p1
            "performance" ->     return R.drawable.p2
            "fast" ->      return R.drawable.p3
            "balance" ->   return R.drawable.p4
            else ->         return R.drawable.p3
        }
    }

    //显示通知
    internal fun notify(msg: String = "辅助服务正在后台运行") {
        if (!showNofity) {
            return
        }
        //获取PendingIntent
        val mainIntent = Intent(context, ActivityMain::class.java)
        val mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager!!.createNotificationChannel(NotificationChannel("vtool", "微工具箱", NotificationManager.IMPORTANCE_LOW))
            notification =
                    Notification.Builder(context, "vtool")
                            .setSmallIcon(R.drawable.fanbox)
                            .setContentTitle(context.getString(R.string.app_name))
                            .setContentText(msg)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            //.setDefaults(Notification.DEFAULT_SOUND)
                            .setContentIntent(mainPendingIntent)
                            .build()
        } else {
            notification =
                    Notification.Builder(context)
                            .setSmallIcon(R.drawable.fanbox)
                            .setContentTitle(context.applicationInfo.name)
                            .setContentText(msg)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            //.setDefaults(Notification.DEFAULT_SOUND)
                            .setContentIntent(mainPendingIntent)
                            .build()
        }

        notification!!.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        notificationManager?.notify(0x100, notification)
    }

    //显示通知 方便快速切换配置模式
    private fun notify(msg: String = "辅助服务正在后台运行", stringPackage: String, icon: Int? = null) {
        if (!showNofity) {
            return
        }

        //获取PendingIntent
        val mainIntent = Intent(context, ActivityQuickSwitchMode::class.java)
        mainIntent.putExtra("packageName", stringPackage)
        val mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager!!.createNotificationChannel(NotificationChannel("vtool", "微工具箱", NotificationManager.IMPORTANCE_LOW))
            notification =
                    Notification.Builder(context, "vtool")
                            .setSmallIcon(if(icon == null) R.drawable.fanbox else icon)
                            .setContentTitle(context.getString(R.string.app_name))
                            .setContentText(msg)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            //.setDefaults(Notification.DEFAULT_SOUND)
                            .setContentIntent(mainPendingIntent)
                            .build()
        } else {
            notification =
                    Notification.Builder(context)
                            .setSmallIcon(if(icon == null) R.drawable.fanbox else icon)
                            .setContentTitle(context.applicationInfo.name)
                            .setContentText(msg)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            //.setDefaults(Notification.DEFAULT_SOUND)
                            .setContentIntent(mainPendingIntent)
                            .build()
        }

        notification!!.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        notificationManager?.notify(0x100, notification)
    }

    public fun notifyPowerModeChange(packageName: String, mode: String) {
        notify("${getAppName(packageName)} -> ${getModName(mode)}",packageName , getModIcon(mode))
    }

    //隐藏通知
    internal fun hideNotify() {
        if (notification != null) {
            notificationManager?.cancel(0x100)
            notification = null
            notificationManager = null
        }
    }

    internal fun setNotify(show:Boolean) {
        this.showNofity = show
        if (!show) {
            hideNotify()
        } else {
            notify()
        }
    }

    init {
        showNofity = notify
    }
}
