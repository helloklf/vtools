package com.omarea.scene_mode

import android.content.Context
import android.content.Intent
import com.omarea.data.EventType
import com.omarea.data.IEventReceiver
import com.omarea.library.calculator.GetUpTime
import com.omarea.store.TriggerStorage

class TriggerIEventMonitor(private val context: Context, override val isAsync: Boolean = false) : IEventReceiver {
    private val triggerListConfig = context.getSharedPreferences("scene_trigger_list", Context.MODE_PRIVATE)

    override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
        val eventName = eventType.name

        // 根据事件筛选
        val items = triggerListConfig.all.filter {
            (it.value as String).contains(eventName)
        }.keys

        val storage = TriggerStorage(context)
        val triggers = ArrayList<String>()
        items.forEach {
            val trigger = storage.load(it)
            if (trigger != null && trigger.enabled) {
                if (trigger.timeLimited) {
                    val nowTimeValue = GetUpTime(trigger.timeStart).currentTime
                    val getUp = trigger.timeEnd
                    val sleep = trigger.timeStart

                    val inTimeSection =
                            // 如果【起床时间】比【睡觉时间】要大，如 2:00 睡到 9:00 起床
                            (getUp > sleep && (nowTimeValue >= sleep && nowTimeValue <= getUp)) ||
                                    // 正常时间睡觉【睡觉时间】大于【起床时间】，如 23:00 睡到 7:00 起床
                                    (getUp < sleep && (nowTimeValue >= sleep || nowTimeValue <= getUp))
                    if (!inTimeSection) {
                        return
                    }
                }
                triggers.add(it)
            }
        }
        if (triggers.size > 0) {
            startExecutor(triggers)
        }
    }

    override fun onSubscribe() {

    }

    override fun onUnsubscribe() {

    }

    private fun startExecutor(triggers: ArrayList<String>) {
        val taskIntent = Intent(context, TriggerExecutorService::class.java)
        taskIntent.putExtra("triggers", triggers)
        context.startService(taskIntent)
    }

    override fun eventFilter(eventType: EventType): Boolean {
        when (eventType) {
            EventType.SCREEN_ON,
            EventType.SCREEN_OFF,
            EventType.BOOT_COMPLETED,
            EventType.BATTERY_LOW,
            EventType.POWER_CONNECTED,
            EventType.POWER_DISCONNECTED -> {
                return true
            }
            else -> return false
        }
    }
}

