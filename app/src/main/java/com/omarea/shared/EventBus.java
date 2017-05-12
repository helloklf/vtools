package com.omarea.shared;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Hello on 2017/4/8.
 */

public class EventBus {
    private static HashMap<String, List<IEventSubscribe>> subscribes;

    public static void subscribe(String eventName, final IEventSubscribe runnable) {
        if (subscribes == null)
            subscribes = new HashMap<>();

        if (subscribes.containsKey(eventName))
            subscribes.get(eventName).add(runnable);
        else
            subscribes.put(eventName, new ArrayList<IEventSubscribe>() {{
                add(runnable);
            }});
    }

    public static void unSubscribe(String eventName, final IEventSubscribe runnable) {
        if (subscribes == null)
            return;
        if (subscribes.containsKey(eventName))
            subscribes.get(eventName).remove(runnable);
    }

    public static void publish(String eventName, final Object message) {
        if (subscribes == null)
            return;
        List<IEventSubscribe> handlers = subscribes.get(eventName);

        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).messageRecived(message);
        }
    }

    public static void publish(String eventName) {
        if (subscribes == null)
            return;
        List<IEventSubscribe> handlers = subscribes.get(eventName);

        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).messageRecived(null);
        }
    }
}
