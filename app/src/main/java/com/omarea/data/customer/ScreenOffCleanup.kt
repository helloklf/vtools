package com.omarea.data.customer

import android.content.Context
import com.omarea.Scene
import com.omarea.data.EventType
import com.omarea.data.IEventReceiver
import com.omarea.vtools.popup.FloatMonitor
import com.omarea.vtools.popup.FloatMonitorGame

class ScreenOffCleanup(private val context: Context) : IEventReceiver {
    override fun eventFilter(eventType: EventType): Boolean {
        return eventType == EventType.SCREEN_OFF
    }

    override fun onReceive(eventType: EventType) {
        Scene.post(Runnable{
            FloatMonitorGame(context).hidePopupWindow()
            FloatMonitor(context).hidePopupWindow()
        })
    }
    override val isAsync: Boolean
        get() = false
}