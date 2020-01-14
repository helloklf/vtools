package com.omarea.data_collection;

import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

public class EventBus {
    private static ArrayList<EventReceiver> eventReceivers;

    /**
     * 发布事件
     * @param eventType 事件类型
     * @param intent    事件内容
     */
    public static void publish(EventTypes eventType, Intent intent) {
        if (eventReceivers != null && eventReceivers.size() > 0) {
            for (EventReceiver eventReceiver : eventReceivers) {
                try {
                    if (eventReceiver.eventFilter(eventType, intent)) {
                        eventReceiver.onReceive(eventType, intent);
                    }
                } catch (Exception ex) {
                    Log.e("SceneEventBus", "" + ex.getMessage());
                }
            }
        }
    }

    /**
     * 订阅事件
     * @param eventReceiver 事件接收器
     */
    public static void subscibe(EventReceiver eventReceiver) {
        if (eventReceivers == null) {
            eventReceivers = new ArrayList<EventReceiver>();
        }

        if (!eventReceivers.contains(eventReceiver)) {
            eventReceivers.add(eventReceiver);
        }
    }
}
