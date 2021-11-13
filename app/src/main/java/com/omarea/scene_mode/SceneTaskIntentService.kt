package com.omarea.scene_mode

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.widget.Toast
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.IEventReceiver
import com.omarea.library.basic.ScreenState
import com.omarea.store.TimingTaskStorage

class SceneTaskIntentService : IntentService("SceneTaskIntentService") {
    override fun onHandleIntent(intent: Intent?) {
        intent?.run {
            val taskId = if (intent.hasExtra("taskId")) intent.getStringExtra("taskId") else null
            taskId?.run {
                executeTask(this)
            }
        }
    }

    private fun executeTask(taskId: String) {
        val context = this
        val timingTask = TimingTaskStorage(context).load(taskId)
        timingTask?.run {
            if (timingTask.expireDate > 0 && timingTask.expireDate <= System.currentTimeMillis()) {
                this.enabled = false
                TimingTaskManager(this@SceneTaskIntentService).setTask(this)
            } else {
                TimingTaskManager(this@SceneTaskIntentService).setTask(this)
            }

            if (chargeOnly && GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                Toast.makeText(context, "未在充电状态，跳过定时任务", Toast.LENGTH_LONG).show()
            } else if (batteryCapacityRequire > 0 && GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING && GlobalStatus.batteryCapacity < batteryCapacityRequire) {
                Toast.makeText(context, "电量低于" + batteryCapacityRequire + "%，跳过定时任务", Toast.LENGTH_LONG).show()
            } else if (afterScreenOff && ScreenState(context).isScreenOn()) {
                // 如果是个要求屏幕关闭后执行的任务，且现在屏幕还在点亮状态，放到息屏事件观测队列中
                EventBus.subscribe(ScreenDelayTaskReceiver(taskId, context.applicationContext))
            } else {
                TaskActionsExecutor(this.taskActions, this.customTaskActions, context).run()
            }
        }
    }

    // 屏幕关闭后才执行的任务
    class ScreenDelayTaskReceiver(private val taskId: String, private val context: Context, override val isAsync: Boolean = false) : IEventReceiver {
        override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
            EventBus.unsubscribe(this)
            val taskIntent = Intent(context, SceneTaskIntentService::class.java)
            taskIntent.putExtra("taskId", taskId)
            taskIntent.action = taskId

            this.context.startService(taskIntent)
        }

        override fun onSubscribe() {

        }

        override fun onUnsubscribe() {

        }

        override fun eventFilter(eventType: EventType): Boolean {
            return eventType == EventType.SCREEN_OFF
        }
    }
}
