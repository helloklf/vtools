package com.omarea.gesture;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.omarea.gesture.shell.ScreenColor;
import com.omarea.gesture.ui.FloatVirtualTouchBar;
import com.omarea.gesture.ui.TouchIconCache;
import com.omarea.gesture.util.ForceHideNavBarThread;
import com.omarea.gesture.util.GlobalState;
import com.omarea.gesture.util.Overscan;
import com.omarea.gesture.util.Recents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccessibilitySceneGesture extends AccessibilityService {
    public Recents recents = new Recents();
    boolean isLandscapf = false;
    private FloatVirtualTouchBar floatVitualTouchBar = null;
    private BroadcastReceiver configChanged = null;
    private BroadcastReceiver serviceDisable = null;
    private BroadcastReceiver screenStateReceiver;
    private SharedPreferences config;
    private SharedPreferences appSwitchBlackList;
    private ContentResolver cr = null;
    private String lastApp = "";

    private void startForeground() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("fg", "辅助服务进程", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(notificationChannel);
            Notification notification = new Notification.Builder(this, "fg").setTicker("").setSmallIcon(R.drawable.gesture_logo).build();
            startForeground(1, notification);
        } else {
            Notification notification = new Notification.Builder(this).setTicker("").setSmallIcon(R.drawable.gesture_logo).build();
            //id 不能为0
            startForeground(1, notification);
        }
    }

    private void hidePopupWindow() {
        if (floatVitualTouchBar != null) {
            floatVitualTouchBar.hidePopupWindow();
            floatVitualTouchBar = null;
        }
    }

    private void forceHideNavBar() {
        if (Build.MANUFACTURER.equals("samsung") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (cr == null) {
                cr = getContentResolver();
            }
            if (config.getBoolean(SpfConfig.SAMSUNG_OPTIMIZE, SpfConfig.SAMSUNG_OPTIMIZE_DEFAULT)) {
                new ForceHideNavBarThread(cr).run();
            }
        } else {
            if (config.getBoolean(SpfConfig.OVERSCAN_SWITCH, SpfConfig.OVERSCAN_SWITCH_DEFAULT)) {
                new Overscan().setOverscan(this);
            }
        }
    }

    private void resumeNavBar() {
        if (Build.MANUFACTURER.equals("samsung") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (cr == null) {
                    cr = getContentResolver();
                }
                // oneui 策略取消强制禁用手势（因为锁屏唤醒后底部会触摸失灵，需要重新开关）
                Settings.Global.putInt(cr, "navigation_bar_gesture_disabled_by_policy", 0);
            } catch (Exception ex) {
            }
            cr = null;
        } else {
            if (config.getBoolean(SpfConfig.OVERSCAN_SWITCH, SpfConfig.OVERSCAN_SWITCH_DEFAULT)) {
                new Overscan().setOverscan(this);
            }
        }
    }

    private boolean ignored(String packageName) {
        return recents.ignoreApps.indexOf(packageName) > -1;
    }

    /*
    通过
    dumpsys activity top | grep ACTIVITY
    可以获取当前打开的应用，但是，作为普通应用并且有这个权限
    */

    /*
    List<AccessibilityWindowInfo> windowInfos = accessibilityService.getWindows();
    Log.d("AccessibilityWindowInfo", "windowInfos " + windowInfos.size());
    for (AccessibilityWindowInfo windowInfo : windowInfos) {
        try {
            Log.d("AccessibilityWindowInfo", "" + windowInfo.getRoot().getPackageName());
        } catch (Exception ex) {
            Log.e("AccessibilityWindowInfo", "" + ex.getMessage());
        }
    }
    */

    // 检测应用是否是可以打开的
    private boolean canOpen(String packageName) {
        if (recents.blackList.indexOf(packageName) > -1) {
            return false;
        } else if (recents.whiteList.indexOf(packageName) > -1) {
            return true;
        } else {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                recents.whiteList.add(packageName);
                return true;
            } else {
                recents.blackList.add(packageName);
                return false;
            }
        }
    }

    // 启动器应用（桌面）
    private ArrayList<String> getLauncherApps() {
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveinfoList = getPackageManager().queryIntentActivities(resolveIntent, 0);
        ArrayList<String> launcherApps = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveinfoList) {
            launcherApps.add(resolveInfo.activityInfo.packageName);
        }
        return launcherApps;
    }

    // 输入法应用
    private ArrayList<String> getInputMethods() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        ArrayList<String> inputMethods = new ArrayList<>();
        for (InputMethodInfo inputMethodInfo : imm.getInputMethodList()) {
            recents.ignoreApps.add(inputMethodInfo.getPackageName());
        }
        return inputMethods;
    }

    // TODO:判断是否进入全屏状态，以便在游戏和视频过程中降低功耗
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null) {
            if (recents.ignoreApps == null) {
                recents.ignoreApps = getLauncherApps();
                recents.inputMethods = getInputMethods();
                recents.ignoreApps.addAll(recents.inputMethods);
            }

            boolean isWCC = event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            CharSequence packageName = event.getPackageName();
            if (!(packageName == null || packageName.equals(getPackageName()))) {
                String packageNameStr = packageName.toString();

                if (GlobalState.updateBar != null &&
                        !((packageNameStr.equals("com.android.systemui") || (recents.inputMethods.indexOf(packageNameStr) > -1 && recents.inputMethods.indexOf(lastApp) > -1)))) {
                    if (!(packageName.equals("android") || packageName.equals("com.omarea.vtools") || packageName.equals("com.omarea.filter"))) {
                        ScreenColor.updateBarColor(!isWCC);
                        Log.d(">>>>", "" + packageName + " CC");
                    }

                    if (isWCC) {
                        return;
                    }
                }

                if (!ignored(packageNameStr) && canOpen(packageNameStr) && !appSwitchBlackList.contains(packageNameStr)) {
                    recents.addRecent(packageNameStr);
                }
                lastApp = packageNameStr;
            }
        }
    }

    private boolean isScreenLocked() {
        try {
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            if (display.getState() != Display.STATE_ON) {
                return true;
            }

            KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                return mKeyguardManager.inKeyguardRestrictedInputMode() || mKeyguardManager.isDeviceLocked() || mKeyguardManager.isKeyguardLocked();
            } else {
                return mKeyguardManager.inKeyguardRestrictedInputMode() || mKeyguardManager.isKeyguardLocked();
            }
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        if (config == null) {
            config = getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE);
        }
        if (appSwitchBlackList == null) {
            appSwitchBlackList = getSharedPreferences(SpfConfig.AppSwitchBlackList, Context.MODE_PRIVATE);
        }

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        GlobalState.displayWidth = point.x;
        GlobalState.displayHeight = point.y;

        TouchIconCache.setContext(this.getBaseContext());

        if (configChanged == null) {
            configChanged = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null && intent.getAction() != null && intent.getAction().equals(getString(R.string.app_switch_changed))) {
                        if (recents != null) {
                            recents.clear();
                            Toast.makeText(getApplicationContext(), "OK！", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        createPopupView();
                    }
                }
            };

            registerReceiver(configChanged, new IntentFilter(getString(R.string.action_config_changed)));
            registerReceiver(configChanged, new IntentFilter(getString(R.string.app_switch_changed)));
        }
        if (serviceDisable == null) {
            serviceDisable = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        disableSelf();
                    }
                    stopSelf();
                }
            };
            registerReceiver(serviceDisable, new IntentFilter(getString(R.string.action_service_disable)));
        }
        createPopupView();

        screenStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String action = intent.getAction();
                    if (action != null &&
                            ((action.equals(Intent.ACTION_USER_PRESENT) || action.equals(Intent.ACTION_USER_UNLOCKED))
                                    // || action.equals(Intent.ACTION_SCREEN_ON)
                            )) {
                        forceHideNavBar();
                    }
                }
            }
        };
        registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        }
        registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        forceHideNavBar();

        Collections.addAll(recents.blackList, getResources().getStringArray(R.array.app_switch_black_list));
        // startForeground();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        hidePopupWindow();
        resumeNavBar();
        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {

    }

    // 监测屏幕旋转
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (floatVitualTouchBar != null && newConfig != null) {
            isLandscapf = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;

            // 如果分辨率变了，那就重新创建手势区域
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Point point = new Point();
            wm.getDefaultDisplay().getRealSize(point);
            if (point.x != GlobalState.displayWidth || point.y != GlobalState.displayHeight) {
                // 分辨率改变时 屏幕取色重启进程
                if (GlobalState.displayWidth * GlobalState.displayHeight != point.x * point.y) {
                    ScreenColor.stopProcess();
                }

                GlobalState.displayWidth = point.x;
                GlobalState.displayHeight = point.y;
                createPopupView();
                forceHideNavBar();
            }
        }
    }

    private void createPopupView() {
        hidePopupWindow();

        AccessibilityServiceInfo accessibilityServiceInfo = getServiceInfo();
        boolean barEnabled = isLandscapf ? config.getBoolean(SpfConfig.LANDSCAPE_IOS_BAR, SpfConfig.LANDSCAPE_IOS_BAR_DEFAULT) : config.getBoolean(SpfConfig.PORTRAIT_IOS_BAR, SpfConfig.PORTRAIT_IOS_BAR_DEFAULT);
        // 是否激进模式
        if (barEnabled &&
                config.getBoolean(SpfConfig.ROOT, SpfConfig.ROOT_DEFAULT) &&
                config.getBoolean(SpfConfig.IOS_BAR_AUTO_COLOR_ROOT, SpfConfig.IOS_BAR_AUTO_COLOR_ROOT_DEFAULT) &&
                config.getBoolean(SpfConfig.IOS_BAR_COLOR_FAST, SpfConfig.IOS_BAR_COLOR_FAST_DEFAULT)
        ) {
            accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOWS_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        } else {
            accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        }
        setServiceInfo(accessibilityServiceInfo);

        floatVitualTouchBar = new FloatVirtualTouchBar(this, isLandscapf);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatVitualTouchBar != null) {
            floatVitualTouchBar.hidePopupWindow();
        }

        if (configChanged != null) {
            unregisterReceiver(configChanged);
            configChanged = null;
        }

        if (screenStateReceiver != null) {
            unregisterReceiver(screenStateReceiver);
            screenStateReceiver = null;
        }

        // stopForeground(true);
    }
}
