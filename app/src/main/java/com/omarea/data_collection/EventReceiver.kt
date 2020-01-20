package com.omarea.data_collection

interface EventReceiver {
    fun eventFilter(eventType: EventType): Boolean
    fun onReceive(eventType: EventType)
}