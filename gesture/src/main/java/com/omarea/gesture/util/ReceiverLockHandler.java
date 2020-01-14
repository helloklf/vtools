package com.omarea.gesture.util;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Message;
import android.view.View;

public class ReceiverLockHandler extends Handler {
    private View bar;
    private AccessibilityService accessibilityService;

    public ReceiverLockHandler(View bar, AccessibilityService accessibilityService) {
        this.bar = bar;
        this.accessibilityService = accessibilityService;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        try {
            if (bar.isAttachedToWindow()) {
                if (msg != null) {
                    if (msg.what == ReceiverLock.EVENT_SCREEN_ON) {
                        bar.setVisibility(View.VISIBLE);
                    } else if (msg.what == ReceiverLock.EVENT_SCREEN_OFF) {
                        bar.setVisibility(View.GONE);
                    }
                }
            } else {
                ReceiverLock.unRegister(accessibilityService);
            }
        } catch (Exception ignored) {
        }
    }
}
