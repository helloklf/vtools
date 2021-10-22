package com.omarea.data

interface IEventReceiver {
    fun eventFilter(eventType: EventType): Boolean
    fun onReceive(eventType: EventType)
    val isAsync: Boolean;
    fun onSubscribe()
    fun onUnsubscribe()
}