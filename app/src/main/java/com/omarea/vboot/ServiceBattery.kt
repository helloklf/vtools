package com.omarea.vboot

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder

class ServiceBattery : Service() {

    internal var batteryChangedReciver: ReciverBatterychanged? = null


    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        // throw UnsupportedOperationException("Not yet implemented")
        return null
    }

    override fun onCreate() {
        if (batteryChangedReciver == null) {
            //监听电池改变
            batteryChangedReciver = ReciverBatterychanged(this)
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
            batteryChangedReciver!!.resumeCharge()
            batteryChangedReciver!!.entryFastChanger(true)
        }
    }

    override fun onDestroy() {
        try {
            if (batteryChangedReciver != null) {
                batteryChangedReciver!!.onDestroy()
                unregisterReceiver(batteryChangedReciver)
                batteryChangedReciver = null
            }
        } catch (igoned: Exception) {

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
