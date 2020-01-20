package com.omarea.scene_mode

import android.content.Context
import android.os.Looper
import com.omarea.data_collection.EventReceiver
import com.omarea.data_collection.EventType
import com.omarea.store.TriggerStorage

class TriggerEventMonitor(private val context: Context) : EventReceiver {
    private val triggerListConfig = context.getSharedPreferences("scene_trigger_list", Context.MODE_PRIVATE)

    override fun onReceive(eventType: EventType) {
        val eventName = eventType.name

        // 根据事件筛选
        val items = triggerListConfig.all.filter {
            (it.value as String).contains(eventName)
        }.keys

        val storage = TriggerStorage(context)
        items.forEach {
            val trigger = storage.load(it)
            if (trigger != null && trigger.enabled && trigger.taskActions != null && trigger.taskActions.size > 0) {
                TimingTaskExecutor(trigger.taskActions, context).run()
            }
        }
    }

    override fun eventFilter(eventType: EventType): Boolean {
        when (eventType) {
            EventType.SCREEN_ON,
            EventType.SCREEN_OFF,
            EventType.BOOT_COMPLETED,
            EventType.BATTERY_LOW,
            EventType.POWER_CONNECTED -> {
                return true
            }
            else -> return false
        }
    }
}

