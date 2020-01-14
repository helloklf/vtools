package com.omarea.gesture.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.omarea.gesture.AccessibilitySceneGesture;
import com.omarea.gesture.ActionModel;
import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;
import com.omarea.gesture.util.GlobalState;

public class FloatVirtualTouchBar {
    private static WindowManager mWindowManager = null;
    private boolean islandscape;
    private View iosBarView = null;
    private View bottomView = null;
    private View leftView = null;
    private View rightView = null;
    private SharedPreferences config;

    public FloatVirtualTouchBar(AccessibilitySceneGesture context, boolean islandscape) {
        this.islandscape = islandscape;

        config = context.getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE);
        mWindowManager = (WindowManager) (context.getSystemService(Context.WINDOW_SERVICE));
        try {
            if (islandscape) {
                if (config.getBoolean(SpfConfig.THREE_SECTION_LANDSCAPE, SpfConfig.THREE_SECTION_LANDSCAPE_DEFAULT)) {
                    this.bottomView = setThreeSectionView(context);
                } else if (config.getBoolean(SpfConfig.CONFIG_BOTTOM_ALLOW_LANDSCAPE, SpfConfig.CONFIG_BOTTOM_ALLOW_LANDSCAPE_DEFAULT)) {
                    this.bottomView = setBottomView(context);
                }
                if (config.getBoolean(SpfConfig.CONFIG_LEFT_ALLOW_LANDSCAPE, SpfConfig.CONFIG_LEFT_ALLOW_LANDSCAPE_DEFAULT)) {
                    this.leftView = setLeftView(context);
                }
                if (config.getBoolean(SpfConfig.CONFIG_RIGHT_ALLOW_LANDSCAPE, SpfConfig.CONFIG_RIGHT_ALLOW_LANDSCAPE_DEFAULT)) {
                    this.rightView = setRightView(context);
                }
                if (config.getBoolean(SpfConfig.LANDSCAPE_IOS_BAR, SpfConfig.LANDSCAPE_IOS_BAR_DEFAULT)) {
                    this.iosBarView = new iOSWhiteBar(context, islandscape).getView();
                }
            } else {
                if (config.getBoolean(SpfConfig.THREE_SECTION_PORTRAIT, SpfConfig.THREE_SECTION_PORTRAIT_DEFAULT)) {
                    this.bottomView = setThreeSectionView(context);
                } else if (config.getBoolean(SpfConfig.CONFIG_BOTTOM_ALLOW_PORTRAIT, SpfConfig.CONFIG_BOTTOM_ALLOW_PORTRAIT_DEFAULT)) {
                    this.bottomView = setBottomView(context);
                }
                if (config.getBoolean(SpfConfig.CONFIG_LEFT_ALLOW_PORTRAIT, SpfConfig.CONFIG_LEFT_ALLOW_PORTRAIT_DEFAULT)) {
                    this.leftView = setLeftView(context);
                }
                if (config.getBoolean(SpfConfig.CONFIG_RIGHT_ALLOW_PORTRAIT, SpfConfig.CONFIG_RIGHT_ALLOW_PORTRAIT_DEFAULT)) {
                    this.rightView = setRightView(context);
                }
                if (config.getBoolean(SpfConfig.PORTRAIT_IOS_BAR, SpfConfig.PORTRAIT_IOS_BAR_DEFAULT)) {
                    this.iosBarView = new iOSWhiteBar(context, islandscape).getView();
                }
            }
        } catch (Exception ex) {
            Toast.makeText(context, "启动虚拟导航手势失败！", Toast.LENGTH_LONG).show();
            // throw  ex;
        }
    }

    int getNavBarHeight(Context context) {
        int resourceId = 0;
        int rid = context.getResources().getIdentifier("config_showNavigationBar", "bool", "android");
        if (rid != 0) {
            resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            return context.getResources().getDimensionPixelSize(resourceId);
        } else {
            return 0;
        }
    }

    /**
     * 隐藏弹出框
     */
    public void hidePopupWindow() {
        if (this.iosBarView != null) {
            mWindowManager.removeView(this.iosBarView);
            this.iosBarView = null;
            GlobalState.iosBarColor = -1;
            GlobalState.updateBar = null;
        }
        if (this.bottomView != null) {
            mWindowManager.removeView(this.bottomView);
            this.bottomView = null;
        }
        if (this.leftView != null) {
            mWindowManager.removeView(this.leftView);
            this.leftView = null;
        }
        if (this.rightView != null) {
            mWindowManager.removeView(this.rightView);
            this.rightView = null;
        }
        // KeepShellPublic.doCmdSync("wm overscan reset");
    }

    /**
     * dp转换成px
     */
    private int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        int value = (int) (dpValue * scale + 0.5f);
        if (value < 1) {
            return 1;
        }
        return value;
    }

    private View setBottomView(final AccessibilitySceneGesture context) {
        final View view = LayoutInflater.from(context).inflate(R.layout.gesture_fw_vitual_touch_bar, null);

        TouchBarView bar = view.findViewById(R.id.bottom_touch_bar);
        if (GlobalState.testMode) {
            bar.setBackground(context.getDrawable(R.drawable.bar_background));
            // bar.setBackgroundColor(Color.argb(128, 0, 0, 0));
        }

        bar.setEventHandler(
                ActionModel.getConfig(config, SpfConfig.CONFIG_BOTTOM_EVENT, SpfConfig.CONFIG_BOTTOM_EVENT_DEFAULT),
                ActionModel.getConfig(config, SpfConfig.CONFIG_BOTTOM_EVENT_HOVER, SpfConfig.CONFIG_BOTTOM_EVENT_HOVER_DEFAULT),
                context);

        double widthRatio = config.getInt(SpfConfig.CONFIG_BOTTOM_WIDTH, SpfConfig.CONFIG_BOTTOM_WIDTH_DEFAULT) / 100.0;

        // 横屏缩小宽度，避免游戏误触
        if (islandscape && widthRatio > 0.4) {
            widthRatio = 0.4;
        }

        int barWidth = (int) (getScreenWidth(context) * widthRatio);
        int barHeight;
        if (islandscape) {
            barHeight = dp2px(context, config.getInt(SpfConfig.CONFIG_HOT_SIDE_WIDTH, SpfConfig.CONFIG_HOT_SIDE_WIDTH_DEFAULT)); // LayoutParams.WRAP_CONTENT;
        } else {
            barHeight = dp2px(context, config.getInt(SpfConfig.CONFIG_HOT_BOTTOM_HEIGHT, SpfConfig.CONFIG_HOT_BOTTOM_HEIGHT_DEFAULT)); // LayoutParams.WRAP_CONTENT;
        }

        bar.setBarPosition(TouchBarView.BOTTOM, islandscape, config.getBoolean(SpfConfig.GAME_OPTIMIZATION, SpfConfig.GAME_OPTIMIZATION_DEFAULT), barWidth, barHeight);

        final LayoutParams params = new LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            params.type = LayoutParams.TYPE_SYSTEM_ALERT;
        }

        params.format = PixelFormat.TRANSLUCENT;

        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        mWindowManager.addView(view, params);

        return view;
    }

    private View setThreeSectionView(final AccessibilitySceneGesture context) {
        final View view = LayoutInflater.from(context).inflate(R.layout.gesture_fw_three_section, null);

        ThreeSectionView bar = view.findViewById(R.id.core_area);
        if (GlobalState.testMode) {
            bar.setBackground(context.getDrawable(R.drawable.bar_background));
        }

        bar.setEventHandler(
                ActionModel.getConfig(config, SpfConfig.THREE_SECTION_LEFT_SLIDE, SpfConfig.THREE_SECTION_LEFT_SLIDE_DEFAULT),
                ActionModel.getConfig(config, SpfConfig.THREE_SECTION_LEFT_HOVER, SpfConfig.THREE_SECTION_LEFT_HOVER_DEFAULT),
                ActionModel.getConfig(config, SpfConfig.THREE_SECTION_CENTER_SLIDE, SpfConfig.THREE_SECTION_CENTER_SLIDE_DEFAULT),
                ActionModel.getConfig(config, SpfConfig.THREE_SECTION_CENTER_HOVER, SpfConfig.THREE_SECTION_CENTER_HOVER_DEFAULT),
                ActionModel.getConfig(config, SpfConfig.THREE_SECTION_RIGHT_SLIDE, SpfConfig.THREE_SECTION_RIGHT_SLIDE_DEFAULT),
                ActionModel.getConfig(config, SpfConfig.THREE_SECTION_RIGHT_HOVER, SpfConfig.THREE_SECTION_RIGHT_HOVER_DEFAULT),
                context);

        double widthRatio = config.getInt(SpfConfig.THREE_SECTION_WIDTH, SpfConfig.THREE_SECTION_WIDTH_DEFAULT) / 100.0;

        // 横屏缩小宽度，避免游戏误触
        if (islandscape && widthRatio > 0.4) {
            widthRatio = 0.4;
        }

        int barWidth = (int) (getScreenWidth(context) * widthRatio);
        int barHeight = dp2px(context, config.getInt(SpfConfig.THREE_SECTION_HEIGHT, SpfConfig.THREE_SECTION_HEIGHT_DEFAULT)); // LayoutParams.WRAP_CONTENT;

        bar.setBarPosition(islandscape, config.getBoolean(SpfConfig.GAME_OPTIMIZATION, SpfConfig.GAME_OPTIMIZATION_DEFAULT), barWidth, barHeight);

        final LayoutParams params = new LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            params.type = LayoutParams.TYPE_SYSTEM_ALERT;
        }

        params.format = PixelFormat.TRANSLUCENT;

        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        mWindowManager.addView(view, params);

        return view;
    }

    private int getScreenHeight(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int h = displayMetrics.heightPixels;
        int w = displayMetrics.widthPixels;
        if (islandscape) {
            return h > w ? w : h;
        } else {
            return h > w ? h : w;
        }
    }

    private int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int h = displayMetrics.heightPixels;
        int w = displayMetrics.widthPixels;
        if (islandscape) {
            return h < w ? w : h;
        } else {
            return h < w ? h : w;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private View setLeftView(final AccessibilitySceneGesture context) {
        final View view = LayoutInflater.from(context).inflate(R.layout.gesture_fw_vitual_touch_bar, null);
        TouchBarView bar = view.findViewById(R.id.bottom_touch_bar);
        if (GlobalState.testMode) {
            // bar.setBackgroundColor(Color.argb(128, 0, 0, 0));
            bar.setBackground(context.getDrawable(R.drawable.bar_background));
        }

        bar.setEventHandler(ActionModel.getConfig(config, SpfConfig.CONFIG_LEFT_EVENT, SpfConfig.CONFIG_LEFT_EVENT_DEFAULT), ActionModel.getConfig(config, SpfConfig.CONFIG_LEFT_EVENT_HOVER, SpfConfig.CONFIG_LEFT_EVENT_HOVER_DEFAULT), context);

        double heightRatio = config.getInt(SpfConfig.CONFIG_LEFT_HEIGHT, SpfConfig.CONFIG_LEFT_HEIGHT_DEFAULT) / 100.0;
        int barHeight = (int) (getScreenHeight(context) * heightRatio); //
        int barWidth = -1;
        if (islandscape) {
            barWidth = dp2px(context, config.getInt(SpfConfig.CONFIG_HOT_BOTTOM_HEIGHT, SpfConfig.CONFIG_HOT_BOTTOM_HEIGHT_DEFAULT)); // LayoutParams.WRAP_CONTENT;
        } else {
            barWidth = dp2px(context, config.getInt(SpfConfig.CONFIG_HOT_SIDE_WIDTH, SpfConfig.CONFIG_HOT_SIDE_WIDTH_DEFAULT)); // LayoutParams.WRAP_CONTENT;
        }

        bar.setBarPosition(TouchBarView.LEFT, islandscape, config.getBoolean(SpfConfig.GAME_OPTIMIZATION, SpfConfig.GAME_OPTIMIZATION_DEFAULT), barWidth, barHeight);

        final LayoutParams params = new LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            params.type = LayoutParams.TYPE_SYSTEM_ALERT;
        }

        params.format = PixelFormat.TRANSLUCENT;

        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;

        params.gravity = Gravity.START | Gravity.BOTTOM;
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        mWindowManager.addView(view, params);
        // view.setOnTouchListener(getTouchPierceListener(params, view));

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    private View setRightView(final AccessibilitySceneGesture context) {
        final View view = LayoutInflater.from(context).inflate(R.layout.gesture_fw_vitual_touch_bar, null);

        TouchBarView bar = view.findViewById(R.id.bottom_touch_bar);
        if (GlobalState.testMode) {
            // bar.setBackgroundColor(Color.argb(128, 0, 0, 0));
            bar.setBackground(context.getDrawable(R.drawable.bar_background));
        }

        bar.setEventHandler(ActionModel.getConfig(config, SpfConfig.CONFIG_RIGHT_EVENT, SpfConfig.CONFIG_RIGHT_EVENT_DEFAULT), ActionModel.getConfig(config, SpfConfig.CONFIG_RIGHT_EVENT_HOVER, SpfConfig.CONFIG_RIGHT_EVENT_HOVER_DEFAULT), context);

        double heightRatio = config.getInt(SpfConfig.CONFIG_RIGHT_HEIGHT, SpfConfig.CONFIG_RIGHT_HEIGHT_DEFAULT) / 100.0;
        int barHeight = (int) (getScreenHeight(context) * heightRatio); //
        int barWidth = -1;
        if (islandscape) {
            barWidth = dp2px(context, config.getInt(SpfConfig.CONFIG_HOT_BOTTOM_HEIGHT, SpfConfig.CONFIG_HOT_BOTTOM_HEIGHT_DEFAULT)); // LayoutParams.WRAP_CONTENT;
        } else {
            barWidth = dp2px(context, config.getInt(SpfConfig.CONFIG_HOT_SIDE_WIDTH, SpfConfig.CONFIG_HOT_SIDE_WIDTH_DEFAULT)); // LayoutParams.WRAP_CONTENT;
        }
        bar.setBarPosition(TouchBarView.RIGHT, islandscape, config.getBoolean(SpfConfig.GAME_OPTIMIZATION, SpfConfig.GAME_OPTIMIZATION_DEFAULT), barWidth, barHeight);

        final LayoutParams params = new LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            params.type = LayoutParams.TYPE_SYSTEM_ALERT;
        }

        params.format = PixelFormat.TRANSLUCENT;

        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;

        params.gravity = Gravity.END | Gravity.BOTTOM;
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        mWindowManager.addView(view, params);
        // view.setOnTouchListener(getTouchPierceListener(params, view));

        return view;
    }
}