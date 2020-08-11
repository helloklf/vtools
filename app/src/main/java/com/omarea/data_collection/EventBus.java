package com.omarea.data_collection;

import android.util.Log;

import java.util.ArrayList;

public class EventBus {
    private static ArrayList<EventReceiver> eventReceivers;

    /**
     * 发布事件
     *
     * @param eventType 事件类型
     */
    public static void publish(EventType eventType) {
        if (eventReceivers != null && eventReceivers.size() > 0) {
            for (EventReceiver eventReceiver : eventReceivers) {
                try {
                    if (eventReceiver.eventFilter(eventType)) {
                        if (eventReceiver.isAsync()) {
                            new HandlerThread(eventReceiver, eventType).start();
                        } else {
                            eventReceiver.onReceive(eventType);
                        }
                    }
                } catch (Exception ex) {
                    Log.e("SceneEventBus", "" + ex.getMessage());
                }
            }
        }
    }

    static class HandlerThread extends Thread {
        private EventReceiver eventReceiver;
        private EventType eventType;

        HandlerThread(EventReceiver eventReceiver, EventType eventType) {
            this.eventReceiver = eventReceiver;
            this.eventType = eventType;
        }

        @Override
        public void run() {
            eventReceiver.onReceive(eventType);
        }
    }

    /**
     * 订阅事件
     *
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
     *
     * @param eventReceiver 事件接收器
     */
    public static void unsubscibe(EventReceiver eventReceiver) {
        if (eventReceivers != null && eventReceivers.indexOf(eventReceiver) > -1) {
            eventReceivers.remove(eventReceiver);
        }
    }
}
