package com.omarea.scene_mode

import android.app.IntentService
import android.content.Intent
import android.content.Context
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.EventReceiver
import com.omarea.data_collection.EventTypes
import com.omarea.model.TimingTaskInfo
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
            if (timingTask.taskActions != null && timingTask.taskActions.size > 0) {
                // 如果是个要求屏幕关闭后执行的任务，且现在屏幕还在点亮状态，放到息屏事件观测队列中
                if (afterScreenOff && ScreenState(context).isScreenOn()) {
                    EventBus.subscibe(ScreenDelayTaskReceiver(this, context.applicationContext))
                } else {
                    TimingTaskExecutor(this, context).run()
                }
            }
        }
    }

    // 屏幕关闭后才执行的任务
    class ScreenDelayTaskReceiver(private val timingTask: TimingTaskInfo, private val context: Context) :EventReceiver {
        override fun onReceive(eventType: EventTypes?) {
            TimingTaskExecutor(timingTask, context).run()
        }

        override fun eventFilter(eventType: EventTypes?): Boolean {
            return eventType == EventTypes.SCREEN_OFF
        }
    }
}
