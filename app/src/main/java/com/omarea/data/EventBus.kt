package com.omarea.data

import android.util.Log
import java.util.*

object EventBus {
    private val eventReceivers = ArrayList<IEventReceiver>()

    /**
     * 发布事件
     *
     * @param eventType 事件类型
     */
    fun publish(eventType: EventType?, data: HashMap<String, Any>? = null) {
        if (eventReceivers.size > 0) {
            // 复制一个副本用于循环，避免在运行过程中unsubscibe致使eventReceivers发生变化而崩溃
            val temp = ArrayList(eventReceivers)
            for (eventReceiver in temp) {
                try {
                    if (eventReceiver.eventFilter(eventType!!)) {
                        if (eventReceiver.isAsync) {
                            HandlerThread(eventReceiver, eventType, data).start()
                        } else {
                            eventReceiver.onReceive(eventType, data)
                        }
                    }
                } catch (ex: Exception) {
                    Log.e("SceneEventBus", "" + ex.message)
                }
            }
        }
    }

    /**
     * 订阅事件
     *
     * @param eventReceiver 事件接收器
     */
    fun subscribe(eventReceiver: IEventReceiver) {
        if (!eventReceivers.contains(eventReceiver)) {
            eventReceivers.add(eventReceiver)
            eventReceiver.onSubscribe()
        }
    }

    /**
     * 取消订阅事件
     *
     * @param eventReceiver 事件接收器
     */
    fun unsubscribe(eventReceiver: IEventReceiver) {
        if (eventReceivers.contains(eventReceiver)) {
            eventReceivers.remove(eventReceiver)
            eventReceiver.onUnsubscribe()
        }
    }

    internal class HandlerThread(private val eventReceiver: IEventReceiver, private val eventType: EventType?, private val data: HashMap<String, Any>?) : Thread() {
        override fun run() {
            eventReceiver.onReceive(eventType!!, data)
        }
    }
}