package com.omarea.data_collection.publisher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.EventTypes
import com.omarea.data_collection.GlobalStatus

class BatteryState : BroadcastReceiver() {
    // 保存状态
    private fun saveState(intent: Intent) {
        GlobalStatus.batteryCapacity = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        GlobalStatus.batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        GlobalStatus.batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f;
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        try {
            saveState(intent);

            val action = intent.action
            GlobalStatus.batteryCapacity = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            GlobalStatus.batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            GlobalStatus.batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f;

            if (action == Intent.ACTION_BATTERY_LOW) {
                EventBus.publish(EventTypes.BATTERY_LOW);
            } else if (action == Intent.ACTION_BATTERY_CHANGED) {
                EventBus.publish(EventTypes.BATTERY_CHANGED);
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                EventBus.publish(EventTypes.CHARGER_DISCONNECTED);
            } else if (action == Intent.ACTION_POWER_CONNECTED) {
                EventBus.publish(EventTypes.POWER_CONNECTED);
            }
        } catch (ex: Exception) {
        } finally {
            pendingResult.finish()
        }
    }
}
