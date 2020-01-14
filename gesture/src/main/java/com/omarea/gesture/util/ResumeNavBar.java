package com.omarea.gesture.util;

import android.content.ContentResolver;
import android.provider.Settings;

public class ResumeNavBar {
    private ContentResolver cr = null;

    public ResumeNavBar(ContentResolver contentResolver) {
        this.cr = contentResolver;
    }

    public void run() {
        try {
            // oneui 策略取消强制禁用手势（因为锁屏唤醒后底部会触摸失灵，需要重新开关）
            Settings.Global.putInt(cr, "navigation_bar_gesture_disabled_by_policy", 0);
        } catch (Exception ignored) {
        }
    }
}