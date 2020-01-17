package com.omarea.data_collection;

public interface EventReceiver {
    public abstract boolean eventFilter(EventTypes eventType);
    public abstract void onReceive(EventTypes eventType);
}
