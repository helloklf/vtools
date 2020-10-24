package com.omarea.data;

import android.util.Log;

import java.util.ArrayList;

public class EventBus {
    private static ArrayList<IEventReceiver> eventReceivers = new ArrayList<>();

    /**
     * 发布事件
     *
     * @param eventType 事件类型
     */
    public static void publish(EventType eventType) {
        if (eventReceivers.size() > 0) {
            // 复制一个副本用于循环，避免在运行过程中unsubscibe致使eventReceivers发生变化而崩溃
            ArrayList<IEventReceiver> temp = new ArrayList<>(eventReceivers);
            for (IEventReceiver eventReceiver : temp) {
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

    /**
     * 订阅事件
     *
     * @param eventReceiver 事件接收器
     */
    public static void subscibe(IEventReceiver eventReceiver) {
        if (eventReceivers.indexOf(eventReceiver) < 0) {
            eventReceivers.add(eventReceiver);
        }
    }

    /**
     * 取消订阅事件
     *
     * @param eventReceiver 事件接收器
     */
    public static void unsubscibe(IEventReceiver eventReceiver) {
        if (eventReceivers.indexOf(eventReceiver) > -1) {
            eventReceivers.remove(eventReceiver);
        }
    }

    static class HandlerThread extends Thread {
        private IEventReceiver eventReceiver;
        private EventType eventType;

        HandlerThread(IEventReceiver eventReceiver, EventType eventType) {
            this.eventReceiver = eventReceiver;
            this.eventType = eventType;
        }

        @Override
        public void run() {
            eventReceiver.onReceive(eventType);
        }
    }
}
