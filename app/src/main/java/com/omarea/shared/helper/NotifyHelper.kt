package com.omarea.shared.helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import com.omarea.vboot.ActivityMain
import com.omarea.vboot.R


/**
 * 用于在通知栏显示通知
 * Created by Hello on 2018/01/23.
 */

internal class NotifyHelper(private var context: Context, notify: Boolean = false) {
    private var showNofity:Boolean = false
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null

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
                            .setSmallIcon(R.drawable.linux)
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
                            .setSmallIcon(R.drawable.linux)
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
    internal fun notify(msg: String = "辅助服务正在后台运行", stringPackage: String) {
        if (!showNofity) {
            return
        }

        //获取PendingIntent
        val mainIntent = Intent(context, ActivityMain::class.java)
        mainIntent.putExtra("packageName", stringPackage)
        val mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager!!.createNotificationChannel(NotificationChannel("vtool", "微工具箱", NotificationManager.IMPORTANCE_LOW))
            notification =
                    Notification.Builder(context, "vtool")
                            .setSmallIcon(R.drawable.linux)
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
                            .setSmallIcon(R.drawable.linux)
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
