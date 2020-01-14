package com.omarea.gesture.util;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import com.omarea.gesture.AccessibilitySceneGesture;
import com.omarea.gesture.ActionModel;
import com.omarea.gesture.AppSwitchActivity;
import com.omarea.gesture.SpfConfig;
import com.omarea.gesture.SpfConfigEx;
import com.omarea.gesture.shell.KeepShellPublic;
import com.omarea.gesture.ui.QuickPanel;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

public class Handlers {
    final public static int GLOBAL_ACTION_NONE = 0;
    final public static int GLOBAL_ACTION_BACK = AccessibilityService.GLOBAL_ACTION_BACK;
    final public static int GLOBAL_ACTION_HOME = AccessibilityService.GLOBAL_ACTION_HOME;
    final public static int GLOBAL_ACTION_RECENTS = AccessibilityService.GLOBAL_ACTION_RECENTS;
    final public static int GLOBAL_ACTION_NOTIFICATIONS = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS;
    final public static int GLOBAL_ACTION_QUICK_SETTINGS = AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS;
    final public static int GLOBAL_ACTION_POWER_DIALOG = AccessibilityService.GLOBAL_ACTION_POWER_DIALOG;
    final public static int GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN = AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN;
    final public static int GLOBAL_ACTION_LOCK_SCREEN = AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN;
    final public static int GLOBAL_ACTION_TAKE_SCREENSHOT = AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT;
    final public static int VITUAL_ACTION_NEXT_APP = 900000;
    final public static int VITUAL_ACTION_PREV_APP = 900001;
    final public static int VITUAL_ACTION_XIAO_AI = 900002;
    final public static int VITUAL_ACTION_SWITCH_APP = 900005;
    final public static int VITUAL_ACTION_FORM = 900009;

    final public static int CUSTOM_ACTION_APP = 1000001;
    final public static int CUSTOM_ACTION_APP_WINDOW = 1000002;
    final public static int CUSTOM_ACTION_SHELL = 1000006;
    final public static int CUSTOM_ACTION_QUICK = 1000009;

    private static SharedPreferences config;
    private static SharedPreferences configEx;
    private static boolean isXiaomi = Build.MANUFACTURER.equals("Xiaomi") && Build.BRAND.equals("Xiaomi");

    private final static ActionModel[] options = new ArrayList<ActionModel>() {{
        add(new ActionModel(GLOBAL_ACTION_NONE, "无"));
        add(new ActionModel(GLOBAL_ACTION_BACK, "返回键"));
        add(new ActionModel(GLOBAL_ACTION_HOME, "Home键"));
        add(new ActionModel(GLOBAL_ACTION_RECENTS, "任务键"));
        add(new ActionModel(GLOBAL_ACTION_NOTIFICATIONS, "下拉通知"));
        add(new ActionModel(GLOBAL_ACTION_QUICK_SETTINGS, "快捷面板"));
        add(new ActionModel(GLOBAL_ACTION_POWER_DIALOG, "电源菜单"));
        add(new ActionModel(VITUAL_ACTION_PREV_APP, "上个应用"));
        add(new ActionModel(VITUAL_ACTION_NEXT_APP, "下个应用"));

        if (isXiaomi) {
            add(new ActionModel(VITUAL_ACTION_XIAO_AI, "小爱[ROOT]"));
        }

        if (Build.VERSION.SDK_INT > 23) {
            add(new ActionModel(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN, "分屏"));
            add(new ActionModel(VITUAL_ACTION_SWITCH_APP, "上个应用[原生]"));
            add(new ActionModel(VITUAL_ACTION_FORM, "应用窗口化[试验]"));
        }

        if (Build.VERSION.SDK_INT > 27) {
            add(new ActionModel(GLOBAL_ACTION_LOCK_SCREEN, "锁屏"));
            add(new ActionModel(GLOBAL_ACTION_TAKE_SCREENSHOT, "屏幕截图"));
        }

        add(new ActionModel(CUSTOM_ACTION_APP, "EX-打开应用"));
        if (Build.VERSION.SDK_INT > 23) {
            add(new ActionModel(CUSTOM_ACTION_APP_WINDOW, "EX-应用窗口[试验]"));
        }
        add(new ActionModel(CUSTOM_ACTION_SHELL, "EX-运行脚本"));
        add(new ActionModel(CUSTOM_ACTION_QUICK, "EX-常用应用"));
    }}.toArray(new ActionModel[0]);

    private static Process rootProcess = null;
    private static OutputStream rootOutputStream = null;

    // FIXME:
    // <uses-permission android:name="android.permission.STOP_APP_SWITCHES" />
    // 由于Google限制，再按下Home键以后，后台应用如果想要打开Activity则需要等待5秒，参考 stopAppSwitches 相关逻辑
    // 这导致应用切换手势和打开应用的操作变得体验很差
    // 目前还没找到解决办法

    public static void executeVirtualAction(
            final AccessibilitySceneGesture accessibilityService,
            final ActionModel action, float touchRawX, float touchRawY) {
        switch (action.actionCode) {
            case GLOBAL_ACTION_NONE: {
                break;
            }
            case VITUAL_ACTION_SWITCH_APP: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                        try {
                            Thread.sleep(350);
                        } catch (Exception ignored) {
                        }
                        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                    }
                }).start();
                break;
            }
            case VITUAL_ACTION_NEXT_APP:
            case VITUAL_ACTION_PREV_APP:
            case VITUAL_ACTION_FORM:
            case GLOBAL_ACTION_HOME: {
                int animation = accessibilityService
                        .getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE)
                        .getInt(SpfConfig.HOME_ANIMATION, SpfConfig.HOME_ANIMATION_DEFAULT);
                if (action.actionCode == GLOBAL_ACTION_HOME && animation == SpfConfig.HOME_ANIMATION_DEFAULT) {
                    accessibilityService.performGlobalAction(action.actionCode);
                } else {
                    appSwitch(accessibilityService, action.actionCode, animation);
                }
                break;
            }
            case VITUAL_ACTION_XIAO_AI: {
                openXiaoAi();
                break;
            }
            case GLOBAL_ACTION_TAKE_SCREENSHOT: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500);
                        } catch (Exception ignored) {
                        }
                        accessibilityService.performGlobalAction(action.actionCode);
                    }
                }).start();
                break;
            }
            case CUSTOM_ACTION_SHELL: {
                executeShell(accessibilityService, action);
                break;
            }
            case CUSTOM_ACTION_APP:
            case CUSTOM_ACTION_APP_WINDOW: {
                openApp(accessibilityService, action);
                break;
            }
            case CUSTOM_ACTION_QUICK: {
                openQuickPanel(accessibilityService, touchRawX, touchRawY);
                break;
            }
            default: {
                accessibilityService.performGlobalAction(action.actionCode);
                break;
            }
        }
    }

    private static void openQuickPanel(final AccessibilitySceneGesture accessibilityService, float touchRawX, float touchRawY) {
        new QuickPanel(accessibilityService).open((int) touchRawX, (int) touchRawY);
    }

    private static void appSwitch(final AccessibilitySceneGesture accessibilityService, final int action, final int animation) {
        try {
            Intent intent = AppSwitchActivity.getOpenAppIntent(accessibilityService);
            intent.putExtra("animation", animation);

            switch (action) {
                case GLOBAL_ACTION_HOME: {
                    intent.putExtra("home", true);
                    // AppSwitchActivity.backHome(accessibilityService);
                    break;
                }
                case VITUAL_ACTION_FORM: {
                    intent.putExtra("form", accessibilityService.recents.getCurrent());
                    break;
                }
                case VITUAL_ACTION_PREV_APP:
                case VITUAL_ACTION_NEXT_APP: {
                    if (config == null) {
                        config = accessibilityService.getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE);
                    }
                    if (config.getBoolean(SpfConfig.ROOT, SpfConfig.ROOT_DEFAULT)) {
                        // 单个app：dumpsys activity com.android.browser | grep ACTIVITY | cut -F3 | cut -f1 -d '/'
                        // recent： dumpsys activity r | grep TaskRecord | grep A= | cut -F7 | cut -f2 -d '='
                        // top Activity（慢）： dumpsys activity top | grep ACTIVITY | cut -F3 | cut -f1 -d '/'
                        ArrayList<String> recents = new ArrayList<>();
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            Collections.addAll(recents, KeepShellPublic.doCmdSync("dumpsys activity r | grep realActivity | cut -f2 -d '=' | cut -f1 -d '/'").split("\n"));
                        } else {
                            Collections.addAll(recents, KeepShellPublic.doCmdSync("dumpsys activity r | grep mActivityComponent | cut -f2 -d '=' | cut -f1 -d '/'").split("\n"));
                        }
                        accessibilityService.recents.setRecents(recents, accessibilityService);
                    }
                    if (action == VITUAL_ACTION_PREV_APP) {
                        String targetApp = accessibilityService.recents.movePrevious();
                        if (targetApp != null) {
                            intent.putExtra("prev", targetApp);
                        } else {
                            Toast.makeText(accessibilityService, "<<", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        String targetApp = accessibilityService.recents.moveNext();
                        if (targetApp != null) {
                            intent.putExtra("next", targetApp);
                        } else {
                            Toast.makeText(accessibilityService, ">>", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    break;
                }
            }
            accessibilityService.startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(accessibilityService, "AppSwitch Exception >> " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void openApp(AccessibilitySceneGesture accessibilityService, ActionModel action) {
        if (configEx == null) {
            configEx = accessibilityService.getSharedPreferences(SpfConfigEx.configFile, Context.MODE_PRIVATE);
        }

        boolean windowMode = action.actionCode == Handlers.CUSTOM_ACTION_APP_WINDOW;

        String app = configEx.getString((windowMode ? SpfConfigEx.prefix_app_window : SpfConfigEx.prefix_app) + action.exKey, "");
        if (app != null && !app.isEmpty()) {
            try {
                Intent intent = AppSwitchActivity.getOpenAppIntent(accessibilityService);
                if (windowMode) {
                    intent.putExtra("app-window", app);
                } else {
                    intent.putExtra("app", app);
                }
                accessibilityService.startActivity(intent);
                // PendingIntent pendingIntent = PendingIntent.getActivity(accessibilityService.getApplicationContext(), 0, intent, 0);
                // pendingIntent.send();
            } catch (Exception ex) {
                Toast.makeText(accessibilityService, "AppSwitch Exception >> " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static void executeShell(AccessibilitySceneGesture accessibilityService, ActionModel action) {
        if (configEx == null) {
            configEx = accessibilityService.getSharedPreferences(SpfConfigEx.configFile, Context.MODE_PRIVATE);
        }

        String shell = configEx.getString(SpfConfigEx.prefix_shell + action.exKey, "");
        if (shell != null && !shell.isEmpty()) {
            KeepShellPublic.doCmdSync(shell);
        }
    }

    public static String getOption(int value) {
        for (ActionModel actionModel : options) {
            if (actionModel.actionCode == value) {
                return actionModel.title;
            }
        }
        return "";
    }

    public static ActionModel[] getOptions() {
        return options;
    }

    private static void openXiaoAi() {
        /*
         try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            // com.miui.voiceassist/com.xiaomi.voiceassistant.AiSettings.AiShortcutActivity
            ComponentName xiaoAi = new ComponentName("com.miui.voiceassist", "com.xiaomi.voiceassistant.AiSettings.AiShortcutActivity");
            intent.setComponent(xiaoAi);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            accessibilityService.startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(accessibilityService, "" + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        */
        if (rootProcess == null) {
            try {
                rootProcess = Runtime.getRuntime().exec("su");
                rootOutputStream = rootProcess.getOutputStream();
            } catch (Exception ex) {
            }
        }
        if (rootProcess != null && rootOutputStream != null) {
            try {
                rootOutputStream.write("am start -n com.miui.voiceassist/com.xiaomi.voiceassistant.AiSettings.AiShortcutActivity\n".getBytes());
                rootOutputStream.flush();
            } catch (Exception ex) {
                rootProcess = null;
                rootOutputStream = null;
            }
        }
    }
}
