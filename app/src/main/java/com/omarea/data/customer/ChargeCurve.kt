package com.omarea.data.customer

import android.content.Context
import android.os.BatteryManager
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.IEventReceiver
import com.omarea.store.ChargeSpeedStore
import com.omarea.store.SpfConfig
import java.util.*

class ChargeCurve(context: Context) : IEventReceiver {
    private val storage = ChargeSpeedStore(context)
    private var timer: Timer? = null
    private var batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private var globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

    override fun eventFilter(eventType: EventType): Boolean {
        return when (eventType) {
            EventType.POWER_CONNECTED,
            EventType.POWER_DISCONNECTED,
            EventType.BATTERY_CHANGED -> {
                true
            }
            else -> false
        }
    }

    override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
        when (eventType) {
            EventType.POWER_CONNECTED -> {
                val last = storage.lastCapacity()
                if (GlobalStatus.batteryCapacity != -1 && GlobalStatus.batteryCapacity != last) {
                    storage.clearAll()
                }
            }
            EventType.POWER_DISCONNECTED -> {
                cancelUpdate()
            }
            EventType.BATTERY_CHANGED -> {
                if (timer == null && GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    // storage.handleConflics(GlobalStatus.batteryCapacity)

                    startUpdate()
                }
            }
            else -> {
            }
        }
    }

    override val isAsync: Boolean
        get() = true

    override fun onSubscribe() {

    }

    override fun onUnsubscribe() {

    }

    private fun startUpdate() {
        if (timer == null) {
            timer = Timer().apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        saveLog()
                    }
                }, 15000, 1000)
            }
        }
    }

    private fun saveLog() {
        if (GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
            // 电流
            GlobalStatus.batteryCurrentNow = (
                    batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) /
                            globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT)
                    )
            batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE)

            if (Math.abs(GlobalStatus.batteryCurrentNow) > 100) {
                storage.addHistory(
                        GlobalStatus.batteryCurrentNow,
                        GlobalStatus.batteryCapacity,
                        GlobalStatus.updateBatteryTemperature()
                )
            }
        } else {
            cancelUpdate()
        }
    }

    private fun cancelUpdate() {
        timer?.run {
            cancel()
            timer = null
        }
    }
}