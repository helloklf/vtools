package com.omarea.data_collection;

import android.content.Intent;

public interface EventReceiver {
    public abstract boolean eventFilter(EventTypes eventType, Intent intent);
    public abstract void onReceive(EventTypes eventType, Intent intent);
}
