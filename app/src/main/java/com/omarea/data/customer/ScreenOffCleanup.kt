package com.omarea.data.customer

import android.content.Context
import com.omarea.Scene
import com.omarea.data.EventType
import com.omarea.data.IEventReceiver
import com.omarea.vtools.popup.*

class ScreenOffCleanup(private val context: Context) : IEventReceiver {
    override fun eventFilter(eventType: EventType): Boolean {
        return eventType == EventType.SCREEN_OFF || eventType == EventType.SCREEN_ON
    }

    private val status = booleanArrayOf(false, false, false, false, false)
    override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
        Scene.post {
            if (eventType == EventType.SCREEN_OFF) {
                status[0] = FloatMonitorMini.show == true
                status[1] = FloatTaskManager.show == true
                status[2] = FloatMonitorThreads.show == true
                status[3] = FloatFpsWatch.show == true
                status[4] = FloatMonitor.show == true

                FloatMonitorMini(context).hidePopupWindow()
                FloatTaskManager(context).hidePopupWindow()
                FloatFpsWatch(context).hidePopupWindow()
                FloatMonitor(context).hidePopupWindow()
                FloatMonitorThreads(context).hidePopupWindow()
            } else if (eventType == EventType.SCREEN_ON) {
                Scene.postDelayed({
                    if (status[0]) {
                        FloatMonitorMini(context).showPopupWindow()
                        status[0] = false
                    }
                    if (status[1]) {
                        FloatTaskManager(context).showPopupWindow()
                        status[1] = false
                    }
                    if (status[2]) {
                        FloatMonitorThreads(context).showPopupWindow()
                        status[2] = false
                    }
                    if (status[3]) {
                        FloatFpsWatch(context).showPopupWindow()
                        status[2] = false
                    }
                    if (status[4]) {
                        FloatMonitor(context).showPopupWindow()
                        status[3] = false
                    }
                }, 2000)
            }
        }
    }

    override val isAsync: Boolean
        get() = false

    override fun onSubscribe() {

    }

    override fun onUnsubscribe() {

    }
}