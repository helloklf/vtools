package com.omarea.data_collection;

public interface EventReceiver {
    public abstract boolean eventFilter(EventType eventType);
    public abstract void onReceive(EventType eventType);
}
