package com.omarea.gesture.util;

import android.content.ContentResolver;
import android.provider.Settings;

public class ForceHideNavBarThread extends Thread {
    private ContentResolver cr = null;

    public ForceHideNavBarThread(ContentResolver contentResolver) {
        this.cr = contentResolver;
    }

    @Override
    public void run() {
        try {
            // Samsung
            Settings.Global.putInt(cr, "navigation_bar_gesture_while_hidden", 1); // oneui 开启手势模式
            Settings.Global.putInt(cr, "navigation_bar_gesture_hint", 0); // oneui 隐藏手势提示
            Settings.Global.putInt(cr, "navigation_bar_gesture_disabled_by_policy", 0); // oneui 策略取消强制禁用手势（因为锁屏唤醒后底部会触摸失灵，需要重新开关）
            Thread.sleep(300);
            Settings.Global.putInt(cr, "navigation_bar_gesture_disabled_by_policy", 1); // oneui 策略强制禁用手势
            // settings put global policy_control null
            if (Settings.Global.getString(cr, "policy_control").equals("immersive.navigation=*")) {
                Settings.Global.putString(cr, "policy_control", "");
            }
        } catch (java.lang.Exception ignored) {
        }
    }
}