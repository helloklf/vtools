package com.omarea.data_collection.publisher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.omarea.charger_booster.BatteryReceiver
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.EventType
import com.omarea.data_collection.GlobalStatus

class BatteryState(private val applicationContext: Context) : BroadcastReceiver() {
    // 保存状态
    private fun saveState(intent: Intent) {
        GlobalStatus.batteryCapacity = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        GlobalStatus.batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        GlobalStatus.batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f;
    }

    private var currentCapacity = 0
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null) {
            return
        }

        val pendingResult = goAsync()
        try {
            saveState(intent);

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            GlobalStatus.batteryCapacity = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            if (status != GlobalStatus.batteryStatus) {
                GlobalStatus.batteryStatus = status
                if (status.equals(BatteryManager.BATTERY_STATUS_CHARGING)) {
                    EventBus.publish(EventType.POWER_CONNECTED)
                } else if (status.equals(BatteryManager.BATTERY_STATUS_FULL)) {
                    EventBus.publish(EventType.BATTERY_FULL)
                }
            }
            GlobalStatus.batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f

            if (action == Intent.ACTION_BATTERY_LOW) {
                EventBus.publish(EventType.BATTERY_LOW)
            } else if (action == Intent.ACTION_BATTERY_CHANGED) {
                EventBus.publish(EventType.BATTERY_CHANGED)
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                EventBus.publish(EventType.POWER_DISCONNECTED)
            } else if (action == Intent.ACTION_POWER_CONNECTED) {
                EventBus.publish(EventType.POWER_CONNECTED)
            }
            if (currentCapacity != GlobalStatus.batteryCapacity) {
                currentCapacity = GlobalStatus.batteryCapacity
                EventBus.publish(EventType.BATTERY_CAPACITY_CHANGED)
            }
        } catch (ex: Exception) {
        } finally {
            pendingResult.finish()
        }
    }

    fun registerReceiver() {
        val batteryChangedReciver = this
        applicationContext.run {
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
        }
        // 充电控制模块
        EventBus.subscibe(BatteryReceiver(applicationContext))
    }
}
