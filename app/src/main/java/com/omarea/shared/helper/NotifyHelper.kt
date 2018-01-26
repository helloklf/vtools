package com.omarea.shared.helper

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import com.omarea.vboot.ActivityMain
import com.omarea.vboot.R

/**
 * Created by Hello on 2018/01/23.
 */

internal class NotifyHelper(private var context: Context, notify: Boolean = false) {
    internal var showNofity:Boolean = false
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null

    //显示通知
    internal fun ShowNotify(msg: String = "辅助服务正在后台运行") {
        if (!showNofity) {
            return
        }
        //获取PendingIntent
        val mainIntent = Intent(context, ActivityMain::class.java)
        val mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        notification =
                Notification.Builder(context)
                        .setSmallIcon(R.drawable.linux)
                        .setContentTitle("微工具箱")
                        .setContentText(msg)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        //.setDefaults(Notification.DEFAULT_SOUND)
                        .setContentIntent(mainPendingIntent)
                        .build()

        notification!!.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        notificationManager?.notify(0x100, notification)
    }

    //隐藏通知
    internal fun HideNotify() {
        if (notification != null) {
            notificationManager?.cancel(0x100)
            notification = null
            notificationManager = null
        }
    }

    internal fun SetNotify(show:Boolean) {
        this.showNofity = show
        if (!show) {
            HideNotify()
        } else {
            ShowNotify()
        }
    }

    init {
        showNofity = notify
    }
}
