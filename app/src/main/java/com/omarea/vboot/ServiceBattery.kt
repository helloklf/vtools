package com.omarea.vboot

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder

import com.omarea.shared.helper.NotifyHelper

class ServiceBattery : Service() {

    internal var batteryChangedReciver: ReciverBatterychanged? = null


    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        if (batteryChangedReciver == null) {
            //监听电池改变
            batteryChangedReciver = ReciverBatterychanged()
            //启动完成
            val ACTION_BOOT_COMPLETED = IntentFilter(Intent.ACTION_BOOT_COMPLETED)
            registerReceiver(batteryChangedReciver, ACTION_BOOT_COMPLETED)
            //电源连接
            val ACTION_POWER_CONNECTED = IntentFilter(Intent.ACTION_POWER_CONNECTED)
            registerReceiver(batteryChangedReciver, ACTION_POWER_CONNECTED)
            //电源断开
            val ACTION_POWER_DISCONNECTED = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
            registerReceiver(batteryChangedReciver, ACTION_POWER_DISCONNECTED)
            //电量变化
            val ACTION_BATTERY_CHANGED = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryChangedReciver, ACTION_BATTERY_CHANGED)
            //电量不足
            val ACTION_BATTERY_LOW = IntentFilter(Intent.ACTION_BATTERY_LOW)
            registerReceiver(batteryChangedReciver, ACTION_BATTERY_LOW)
        }
    }

    override fun onDestroy() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val mainIntent = Intent(applicationContext, ActivityQuickSwitchMode::class.java)
        val mainPendingIntent = PendingIntent.getActivity(applicationContext, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel("vtool-battery-service", applicationContext.applicationInfo.loadLabel(packageManager), NotificationManager.IMPORTANCE_LOW))
            nm.notify(5, Notification.Builder(this, "vtool-boot")
                    .setSmallIcon(R.drawable.ic_menu_digital)
                    .setSubText(applicationContext.applicationInfo.loadLabel(packageManager))
                    .setContentIntent(mainPendingIntent)
                    .setContentText("充电加速服务后台已被终止！")
                    .build()
            )
        } else {
            nm.notify(5, Notification.Builder(this).setSmallIcon(R.drawable.ic_menu_digital)
                    .setSubText(applicationContext.applicationInfo.loadLabel(packageManager))
                    .setContentIntent(mainPendingIntent)
                    .setContentText("充电加速服务后台已被终止！")
                    .build()
            )
        }

        if (batteryChangedReciver != null) {
            unregisterReceiver(batteryChangedReciver)
            batteryChangedReciver = null
        }
    }

    companion object {

        //服务是否在运行
        fun serviceIsRunning(context: Context): Boolean {
            val m = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            if (m != null) {
                val serviceInfos = m.getRunningServices(5000)
                for (serviceInfo in serviceInfos) {
                    if (serviceInfo.service.packageName == "com.omarea.vboot") {
                        if (serviceInfo.service.className == "com.omarea.vboot.ServiceBattery") {
                            return true
                        }
                    }
                }
            }
            return false
        }
    }
}
