package com.omarea.gesture.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.omarea.gesture.R;
import com.omarea.gesture.util.Handlers;

public class TouchIconCache {
    private static Context mContext;
    private static Bitmap touch_arrow_left, touch_arrow_right, touch_tasks, touch_home, touch_lock, touch_notice, touch_power, touch_settings, touch_split, touch_info, touch_screenshot, touch_switch, touch_jump_previous, touch_jump_next, touch_window, touch_app, touch_grid, touch_shell, touch_app_window; // 图标资源

    public static void setContext(Context context) {
        mContext = context;
    }

    static Bitmap getIcon(int action) {
        switch (action) {
            case Handlers.GLOBAL_ACTION_BACK: {
                if (touch_arrow_left == null && mContext != null) {
                    touch_arrow_left = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_arrow_left);
                }
                return touch_arrow_left;
            }
            case Handlers.GLOBAL_ACTION_HOME: {
                if (touch_home == null && mContext != null) {
                    touch_home = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_home);
                }
                return touch_home;
            }
            case Handlers.GLOBAL_ACTION_RECENTS: {
                if (touch_tasks == null && mContext != null) {
                    touch_tasks = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_tasks);
                }
                return touch_tasks;
            }
            case Handlers.GLOBAL_ACTION_LOCK_SCREEN: {
                if (touch_lock == null && mContext != null) {
                    touch_lock = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_lock);
                }
                return touch_lock;
            }
            case Handlers.GLOBAL_ACTION_NOTIFICATIONS: {
                if (touch_notice == null && mContext != null) {
                    touch_notice = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_notice);
                }
                return touch_notice;
            }
            case Handlers.GLOBAL_ACTION_POWER_DIALOG: {
                if (touch_power == null && mContext != null) {
                    touch_power = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_power);
                }
                return touch_power;
            }
            case Handlers.GLOBAL_ACTION_QUICK_SETTINGS: {
                if (touch_settings == null && mContext != null) {
                    touch_settings = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_settings);
                }
                return touch_settings;
            }
            case Handlers.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN: {
                if (touch_split == null && mContext != null) {
                    touch_split = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_split);
                }
                return touch_split;
            }
            case Handlers.GLOBAL_ACTION_TAKE_SCREENSHOT: {
                if (touch_screenshot == null && mContext != null) {
                    touch_screenshot = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_screenshot);
                }
                return touch_screenshot;
            }
            case Handlers.VITUAL_ACTION_SWITCH_APP: {
                if (touch_switch == null && mContext != null) {
                    touch_switch = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_switch);
                }
                return touch_switch;
            }
            case Handlers.VITUAL_ACTION_PREV_APP: {
                if (touch_jump_previous == null && mContext != null) {
                    touch_jump_previous = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_jump_previous);
                }
                return touch_jump_previous;
            }
            case Handlers.VITUAL_ACTION_NEXT_APP: {
                if (touch_jump_next == null && mContext != null) {
                    touch_jump_next = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_jump_next);
                }
                return touch_jump_next;
            }
            case Handlers.VITUAL_ACTION_FORM: {
                if (touch_window == null && mContext != null) {
                    touch_window = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_window);
                }
                return touch_window;
            }
            case Handlers.CUSTOM_ACTION_APP: {
                if (touch_app == null && mContext != null) {
                    touch_app = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_app);
                }
                return touch_app;
            }
            case Handlers.CUSTOM_ACTION_APP_WINDOW: {
                if (touch_app_window == null && mContext != null) {
                    touch_app_window = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_app_window);
                }
                return touch_app_window;
            }
            case Handlers.CUSTOM_ACTION_SHELL: {
                if (touch_shell == null && mContext != null) {
                    touch_shell = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_shell);
                }
                return touch_shell;
            }
            case Handlers.CUSTOM_ACTION_QUICK: {
                if (touch_grid == null && mContext != null) {
                    touch_grid = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_grid);
                }
                return touch_grid;
            }
        }
        if (touch_info == null && mContext != null) {
            touch_info = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.touch_info);
        }
        return touch_info;
    }
}
