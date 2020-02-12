package com.omarea.vtools.services

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.omarea.charger_booster.BatteryReceiver
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.publisher.BatteryState

public class BatteryService : Service() {
    internal var batteryChangedReciver: BatteryState? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        if (batteryChangedReciver == null) {
            //监听电池改变
            batteryChangedReciver = BatteryState()
            //启动完成
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_BOOT_COMPLETED))
            //电源连接
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
            //电源断开
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
            //电量变化
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            //电量不足
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_BATTERY_LOW))

            // 充电控制模块
            EventBus.subscibe(BatteryReceiver(applicationContext))
        }
    }

    override fun onDestroy() {
        try {
            if (batteryChangedReciver != null) {
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
                val className = BatteryService::class.java.simpleName
                val serviceInfos = m.getRunningServices(5000)
                for (serviceInfo in serviceInfos) {
                    if (serviceInfo.service.packageName == context.packageName) {
                        if (serviceInfo.service.className.endsWith(className)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        //启动电池服务
        fun startBatteryService(context: Context): Boolean {
            try {
                val intent = Intent(context, BatteryService::class.java)
                context.startService(intent)
                return true
            } catch (ex: Exception) {
                return serviceIsRunning(context)
            }
        }
    }
}
