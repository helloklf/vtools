package com.omarea.data_collection;

import android.util.Log;

import java.util.ArrayList;

public class EventBus {
    private static ArrayList<EventReceiver> eventReceivers;

    /**
     * 发布事件
     * @param eventType 事件类型
     */
    public static void publish(EventType eventType) {
        if (eventReceivers != null && eventReceivers.size() > 0) {
            for (EventReceiver eventReceiver : eventReceivers) {
                try {
                    if (eventReceiver.eventFilter(eventType)) {
                        eventReceiver.onReceive(eventType);
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
            eventReceivers = new ArrayList<>();
        }

        if (eventReceivers.indexOf(eventReceiver) < 0) {
            eventReceivers.add(eventReceiver);
        }
    }

    /**
     * 取消订阅事件
     * @param eventReceiver 事件接收器
     */
    public static void unsubscibe(EventReceiver eventReceiver) {
        if (eventReceivers != null && eventReceivers.indexOf(eventReceiver) > -1) {
            eventReceivers.remove(eventReceiver);
        }
    }
}
