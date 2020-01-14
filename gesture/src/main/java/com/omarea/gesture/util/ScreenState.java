package com.omarea.gesture.util;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

public class ScreenState {
    private Context context;

    public ScreenState(Context context) {
        this.context = context;
    }

    public boolean isScreenLocked() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        if (display.getState() == Display.STATE_OFF) {
            return true;
        }

        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return mKeyguardManager.isDeviceLocked() || mKeyguardManager.isKeyguardLocked();
        } else {
            return mKeyguardManager.inKeyguardRestrictedInputMode() || mKeyguardManager.isDeviceLocked() || mKeyguardManager.isKeyguardLocked();
        }
    }

    public boolean isScreenOn() {
        return !isScreenLocked();
    }
}
