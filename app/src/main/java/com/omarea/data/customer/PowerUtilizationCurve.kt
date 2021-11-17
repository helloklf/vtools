package com.omarea.data.customer

import android.content.Context
import android.os.BatteryManager
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.IEventReceiver
import com.omarea.library.basic.ScreenState
import com.omarea.store.PowerUtilizationStore
import com.omarea.store.SpfConfig
import java.util.*

class PowerUtilizationCurve(context: Context) : IEventReceiver {
    private val storage = PowerUtilizationStore(context)
    private val screenState = ScreenState(context)
    private var timer: Timer? = null
    private var batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private var globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

    override fun eventFilter(eventType: EventType): Boolean {
        return when (eventType) {
            EventType.SCREEN_ON,
            EventType.SCREEN_OFF,
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
            EventType.SCREEN_ON -> {
                startUpdate()
            }
            EventType.SCREEN_OFF -> {
                cancelUpdate()
            }
            EventType.POWER_CONNECTED -> {
                cancelUpdate()
            }
            EventType.POWER_DISCONNECTED -> {
                val last = storage.lastCapacity()
                if ((last - GlobalStatus.batteryCapacity) > 1 || GlobalStatus.batteryCapacity > last) {
                    storage.clearAll()
                }
                startUpdate()
            }
            EventType.BATTERY_CHANGED -> {
                startUpdate()
            }
            else -> {
            }
        }
    }

    override val isAsync: Boolean
        get() = true

    override fun onSubscribe() {
        startUpdate()
    }

    override fun onUnsubscribe() {

    }

    private fun startUpdate() {
        if (GlobalStatus.batteryStatus != BatteryManager.BATTERY_STATUS_CHARGING && screenState.isScreenOn()) {
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
    }

    private fun saveLog() {
        if (GlobalStatus.batteryStatus != BatteryManager.BATTERY_STATUS_CHARGING) {
            // 电流
            GlobalStatus.batteryCurrentNow = (
                batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) /
                globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT)
            )
            batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE)

            storage.addHistory(
                GlobalStatus.batteryCurrentNow,
                GlobalStatus.batteryCapacity,
                GlobalStatus.updateBatteryTemperature(),
                screenState.isScreenOn()
            )
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