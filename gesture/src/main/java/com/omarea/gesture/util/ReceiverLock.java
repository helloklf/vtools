package com.omarea.gesture.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;

/**
 * 监听屏幕开关事件
 * Created by Hello on 2018/01/23.
 */

public class ReceiverLock extends BroadcastReceiver {
    public static int EVENT_SCREEN_OFF = 8;
    public static int EVENT_SCREEN_ON = 10;
    private static ReceiverLock receiver = null;
    private Handler callbacks;

    public ReceiverLock(Handler callbacks) {
        this.callbacks = callbacks;
    }

    public static ReceiverLock autoRegister(Context context, Handler callbacks) {
        if (receiver != null) {
            unRegister(context);
        }

        receiver = new ReceiverLock(callbacks);
        Context bc = context.getApplicationContext();

        bc.registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            bc.registerReceiver(receiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        }
        bc.registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        bc.registerReceiver(receiver, new IntentFilter(Intent.ACTION_USER_PRESENT));

        return receiver;
    }

    public static void unRegister(Context context) {
        if (receiver == null) {
            return;
        }
        context.getApplicationContext().unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public void onReceive(final Context p0, Intent p1) {
        if (p1 == null) {
            return;
        }

        String action = p1.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                callbacks.sendMessage(callbacks.obtainMessage(EVENT_SCREEN_OFF));
                // } else if (action.equals(Intent.ACTION_USER_PRESENT) || action.equals(Intent.ACTION_USER_UNLOCKED) || action.equals(Intent.ACTION_SCREEN_ON)) {
            } else if (action.equals(Intent.ACTION_USER_PRESENT) || action.equals(Intent.ACTION_USER_UNLOCKED)) {
                try {
                    callbacks.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!new ScreenState(p0).isScreenLocked()) {
                                callbacks.sendMessage(callbacks.obtainMessage(EVENT_SCREEN_ON));
                            }
                        }
                    }, 1000);
                } catch (Exception ignored) {
                }
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                try {
                    callbacks.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!new ScreenState(p0).isScreenLocked()) {
                                callbacks.sendMessage(callbacks.obtainMessage(EVENT_SCREEN_ON));
                            }
                        }
                    }, 6000);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
