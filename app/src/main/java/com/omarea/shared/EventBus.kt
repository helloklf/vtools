package com.omarea.shared

import android.os.Handler
import android.os.Message

import java.util.ArrayList
import java.util.HashMap

/**
 * Created by Hello on 2017/4/8.
 */

object EventBus {
    private var subscribes: HashMap<String, ArrayList<IEventSubscribe>>? = null

    fun subscribe(eventName: String, runnable: IEventSubscribe) {
        if (subscribes == null)
            subscribes = HashMap<String, ArrayList<IEventSubscribe>>()

        if (subscribes!!.containsKey(eventName))
            subscribes!![eventName]!!.add(runnable)
        else
            subscribes!!.put(eventName, object : ArrayList<IEventSubscribe>() {
                init {
                    add(runnable)
                }
            })
    }

    fun unSubscribe(eventName: String, runnable: IEventSubscribe) {
        if (subscribes == null)
            return
        if (subscribes!!.containsKey(eventName))
            subscribes!![eventName]!!.remove(runnable)
    }

    fun publish(eventName: String, message: Any) {
        if (subscribes == null)
            return
        val handlers = subscribes!![eventName]

        for (i in handlers!!.indices) {
            handlers!!.get(i).messageRecived(message)
        }
    }

    fun publish(eventName: String) {
        if (subscribes == null)
            return
        val handlers = subscribes!![eventName]

        for (i in handlers!!.indices) {
            handlers.get(i).messageRecived(null)
        }
    }
}
