package com.omarea.data_collection

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.omarea.store.ChargeSpeedStore
import java.util.*

class ChargeCurve(private val context: Context) : EventReceiver {
    private val storage = ChargeSpeedStore(context)
    private var timer: Timer? = null

    override fun eventFilter(eventType: EventType): Boolean {
        when (eventType) {
            EventType.POWER_CONNECTED,
            EventType.POWER_DISCONNECTED,
            EventType.BATTERY_CHANGED -> {
                return true
            }
            else -> return false
        }
    }

    override fun onReceive(eventType: EventType) {
        when (eventType) {
            EventType.POWER_CONNECTED -> {
                storage.clearAll()
            }
            EventType.POWER_DISCONNECTED -> {
                timer?.run {
                    cancel()
                    timer = null
                }
            }
            EventType.BATTERY_CHANGED -> {
                if (GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    storage.addHistory(GlobalStatus.batteryCurrentNow, GlobalStatus.batteryCapacity)
                }
            }
            else -> {}
        }
    }
}