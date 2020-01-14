package com.omarea.gesture.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import com.omarea.gesture.AccessibilitySceneGesture;
import com.omarea.gesture.ActionModel;
import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;
import com.omarea.gesture.shell.ScreenColor;
import com.omarea.gesture.util.GlobalState;
import com.omarea.gesture.util.Handlers;
import com.omarea.gesture.util.ReceiverLock;
import com.omarea.gesture.util.ReceiverLockHandler;
import com.omarea.gesture.util.ScreenState;

public class iOSWhiteBar {
    private AccessibilitySceneGesture accessibilityService;
    private SharedPreferences config;
    private Boolean isLandscapf;
    private Vibrator vibrator;
    private float pressure = 0;

    float pressureMin;

    public iOSWhiteBar(AccessibilitySceneGesture accessibilityService, Boolean isLandscapf) {
        this.accessibilityService = accessibilityService;
        this.isLandscapf = isLandscapf;
        config = accessibilityService.getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE);
        pressureMin = config.getFloat(SpfConfig.IOS_BAR_PRESS_MIN, SpfConfig.IOS_BAR_PRESS_MIN_DEFAULT);
    }

    /**
     * 获取当前屏幕方向下的屏幕宽度
     */
    private int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int h = displayMetrics.heightPixels;
        int w = displayMetrics.widthPixels;
        if (isLandscapf) {
            return h < w ? w : h;
        } else {
            return h < w ? h : w;
        }
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

    @SuppressLint("ClickableViewAccessibility")
    public View getView() {
        final WindowManager mWindowManager = (WindowManager) (accessibilityService.getSystemService(Context.WINDOW_SERVICE));

        final View view = LayoutInflater.from(accessibilityService).inflate(R.layout.gesture_fw_ios_touch_bar, null);

        final iOSTouchBarView bar = view.findViewById(R.id.bottom_touch_bar);
        /*
        if (GlobalState.testMode) {
            bar.setBackground(accessibilityService.getDrawable(R.drawable.bar_background));
            // bar.setBackgroundColor(Color.argb(128, 0, 0, 0));
        }
        */

        float widthRatio = 0.3f;
        if (isLandscapf) {
            widthRatio = config.getInt(SpfConfig.IOS_BAR_WIDTH_LANDSCAPE, SpfConfig.IOS_BAR_WIDTH_DEFAULT_LANDSCAPE) / 100f;
        } else {
            widthRatio = config.getInt(SpfConfig.IOS_BAR_WIDTH_PORTRAIT, SpfConfig.IOS_BAR_WIDTH_DEFAULT_PORTRAIT) / 100f;
        }

        final boolean gameOptimization = config.getBoolean(SpfConfig.GAME_OPTIMIZATION, SpfConfig.GAME_OPTIMIZATION_DEFAULT);
        final float fateOutAlpha = (isLandscapf ?
                config.getInt(SpfConfig.IOS_BAR_ALPHA_FADEOUT_LANDSCAPE, SpfConfig.IOS_BAR_ALPHA_FADEOUT_PORTRAIT_DEFAULT) :
                config.getInt(SpfConfig.IOS_BAR_ALPHA_FADEOUT_PORTRAIT, SpfConfig.IOS_BAR_ALPHA_FADEOUT_PORTRAIT_DEFAULT)) / 100f; // 0.2f;
        final int barColor = (isLandscapf ?
                (config.getInt(SpfConfig.IOS_BAR_COLOR_LANDSCAPE, SpfConfig.IOS_BAR_COLOR_LANDSCAPE_DEFAULT)) :
                (config.getInt(SpfConfig.IOS_BAR_COLOR_PORTRAIT, SpfConfig.IOS_BAR_COLOR_PORTRAIT_DEFAULT)));
        final int shadowColor = config.getInt(SpfConfig.IOS_BAR_COLOR_SHADOW, SpfConfig.IOS_BAR_COLOR_SHADOW_DEFAULT); // 阴影颜色
        final int shadowSize = config.getInt(SpfConfig.IOS_BAR_SHADOW_SIZE, SpfConfig.IOS_BAR_SHADOW_SIZE_DEFAULT); // 阴影大小
        final int lineWeight = config.getInt(SpfConfig.IOS_BAR_HEIGHT, SpfConfig.IOS_BAR_HEIGHT_DEFAULT); // 线宽度（百分比）
        final int strokeWidth = config.getInt(SpfConfig.IOS_BAR_STROKE_SIZE, SpfConfig.IOS_BAR_STROKE_SIZE_DEFAULT); // 描边大小
        final int strokeColor = config.getInt(SpfConfig.IOS_BAR_COLOR_STROKE, SpfConfig.IOS_BAR_COLOR_STROKE_DEFAULT); // 描边颜色
        final int marginBottom = config.getInt(SpfConfig.IOS_BAR_MARGIN_BOTTOM, SpfConfig.IOS_BAR_MARGIN_BOTTOM_DEFAULT); // 底部边距
        final int totalHeight = marginBottom + lineWeight + (shadowSize * 2) + (strokeWidth * 2);

        bar.setStyle(
                ((int) (getScreenWidth(accessibilityService) * widthRatio)),
                dp2px(accessibilityService, totalHeight),
                barColor,
                shadowColor,
                shadowSize,
                lineWeight,
                strokeWidth,
                strokeColor);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        params.format = PixelFormat.TRANSLUCENT;

        final int originY = -dp2px(accessibilityService, ((isLandscapf && gameOptimization) ? marginBottom : 0));
        final int originX = 0;

        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.y = originY;

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS; // | WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;

        mWindowManager.addView(view, params);

        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            private float touchStartX = 0F; // 触摸开始位置
            private float touchStartY = 0F; // 触摸开始位置
            private float touchStartRawX = 0F; // 触摸开始位置
            private float touchStartRawY = 0F; // 触摸开始位置
            private boolean isTouchDown = false;
            private boolean isGestureCompleted = false;
            private long gestureStartTime = 0L; // 手势开始时间（是指滑动到一定距离，认定触摸手势生效的时间）
            private boolean isLongTimeGesture = false;
            private float touchCurrentX = 0F; // 当前触摸位置
            private float touchCurrentY = 0F; // 当前触摸位置
            private int FLIP_DISTANCE = dp2px(accessibilityService, 50f); // 触摸灵敏度（滑动多长距离认为是手势）
            private float flingValue = dp2px(accessibilityService, 3f); // 小于此值认为是点击而非滑动
            private int offsetLimitX = dp2px(accessibilityService, 50);
            private int offsetLimitY = dp2px(accessibilityService, 12);
            private int animationScaling = dp2px(accessibilityService, 2); // 手指移动多少像素时动画才移动1像素
            private boolean vibratorRun = false;
            private ValueAnimator fareOutAnimation = null; // 动画程序（淡出）
            private ObjectAnimator objectAnimator = null; // 位置调整动画
            private int slideThresholdY = dp2px(accessibilityService, 5); // 滑动多少像素才认为算是滑动，而非点击
            private int slideThresholdX = dp2px(accessibilityService, 10); // 滑动多少像素才认为算是滑动，而非点击

            private float touchCurrentRawX;
            private float touchCurrentRawY;

            private void performGlobalAction(final ActionModel event) {
                if (accessibilityService != null) {
                    Handlers.executeVirtualAction(accessibilityService, event, touchCurrentRawX, touchCurrentRawY);
                }
            }

            private void setPosition(float x, float y) {
                int limitX = (int) x;
                if (limitX < -offsetLimitX) {
                    limitX = -offsetLimitX;
                } else if (limitX > offsetLimitX) {
                    limitX = offsetLimitX;
                }
                int limitY = (int) y;
                if (limitY < -offsetLimitY) {
                    limitY = -offsetLimitY;
                } else if (limitY > offsetLimitY) {
                    limitY = offsetLimitY;
                }
                params.x = limitX;
                params.y = limitY;
                mWindowManager.updateViewLayout(view, params);
            }

            private void fadeOut() {
                if (fareOutAnimation != null) {
                    fareOutAnimation.cancel();
                }
                fareOutAnimation = ValueAnimator.ofFloat(1f, fateOutAlpha);
                fareOutAnimation.setDuration(1000);
                fareOutAnimation.setInterpolator(new LinearInterpolator());
                fareOutAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        try {
                            bar.setAlpha((float) animation.getAnimatedValue());
                        } catch (Exception ignored) {
                        }
                    }
                });
                fareOutAnimation.setStartDelay(5000);
                fareOutAnimation.start();
            }

            private void animationTo(int x, int y, int duration, Interpolator interpolator) {
                Path path = new Path();
                path.moveTo(params.x, params.y);
                path.lineTo(x, y);
                if (objectAnimator != null) {
                    objectAnimator.cancel();
                    objectAnimator = null;
                }

                objectAnimator = ObjectAnimator.ofInt(params, "x", "y", path);
                objectAnimator.setStartDelay(200);
                objectAnimator.setInterpolator(interpolator);
                objectAnimator.setAutoCancel(true);
                objectAnimator.setDuration(duration);
                objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        animation.getValues();
                        int x = (int) animation.getAnimatedValue("x");
                        int y = (int) animation.getAnimatedValue("y");
                        if (x != params.x || y != params.y) {
                            params.x = x;
                            params.y = y;
                            try {
                                mWindowManager.updateViewLayout(view, params);
                            } catch (Exception ex) {
                                animation.cancel();
                                objectAnimator = null;
                            }
                        }
                    }
                });
                objectAnimator.start();
            }

            private void setPressure(MotionEvent event) {
                float p = event.getPressure();
                if (p > pressure) {
                    pressure = event.getPressure();
                }

                if (pressureMin != SpfConfig.IOS_BAR_PRESS_MIN_DEFAULT && pressure > pressureMin) {
                    if (vibratorRun) {
                        touchVibrator(true);
                        vibratorRun = false;
                    }
                    performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_PRESS, SpfConfig.IOS_BAR_PRESS_DEFAULT));
                    isGestureCompleted = true;
                    clearEffect();
                }
            }

            private long lastTouchDown = 0L;

            private boolean onTouchDown(final MotionEvent event) {
                isTouchDown = true;
                isGestureCompleted = false;
                touchStartX = event.getX();
                touchStartY = event.getY();
                touchStartRawX = event.getRawX();
                touchStartRawY = event.getRawY();
                touchCurrentRawX = event.getRawX();
                touchCurrentRawY = event.getRawY();
                touchCurrentX = event.getX();
                touchCurrentY = event.getY();
                gestureStartTime = 0;
                isLongTimeGesture = false;
                vibratorRun = true;
                final long downTime = event.getDownTime();
                lastTouchDown = downTime;

                if (fareOutAnimation != null) {
                    fareOutAnimation.cancel();
                    fareOutAnimation = null;
                }

                if (objectAnimator != null) {
                    objectAnimator.cancel();
                    objectAnimator = null;
                }

                bar.setAlpha(1f);
                bar.invalidate();

                setPressure(event);
                if (pressureMin == SpfConfig.IOS_BAR_PRESS_MIN_DEFAULT) {
                    bar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 上滑悬停
                            if (isTouchDown && !isGestureCompleted && lastTouchDown == downTime) {
                                if (Math.abs(touchStartRawX - touchCurrentRawX) < slideThresholdX && Math.abs(touchStartRawY - touchCurrentRawY) < slideThresholdY) {
                                    int pressureAction = config.getInt(SpfConfig.IOS_BAR_PRESS, SpfConfig.IOS_BAR_PRESS_DEFAULT);
                                    if (pressureAction != SpfConfig.IOS_BAR_TOUCH_DEFAULT) {
                                        isLongTimeGesture = true;
                                        if (vibratorRun) {
                                            touchVibrator(true);
                                            vibratorRun = false;
                                        }
                                        performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_PRESS, SpfConfig.IOS_BAR_PRESS_DEFAULT));
                                        isGestureCompleted = true;
                                        clearEffect();
                                    }
                                }
                            }
                        }
                    }, 280);
                }

                return true;
            }

            private boolean onTouchMove(MotionEvent event) {
                if (isGestureCompleted || !isTouchDown) {
                    return true;
                }

                touchCurrentRawX = event.getRawX();
                touchCurrentRawY = event.getRawY();

                touchCurrentX = event.getX();
                touchCurrentY = event.getY();

                if (touchStartY - touchCurrentY > FLIP_DISTANCE) {
                    if (gestureStartTime < 1) {
                        final long currentTime = System.currentTimeMillis();
                        gestureStartTime = currentTime;
                        bar.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 上滑悬停
                                if (isTouchDown && !isGestureCompleted && currentTime == gestureStartTime) {
                                    isLongTimeGesture = true;
                                    if (vibratorRun) {
                                        touchVibrator();
                                        vibratorRun = false;
                                    }
                                    performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_SLIDE_UP_HOVER, SpfConfig.IOS_BAR_SLIDE_UP_HOVER_DEFAULT));
                                    isGestureCompleted = true;
                                    clearEffect();
                                }
                            }
                        }, config.getInt(SpfConfig.CONFIG_HOVER_TIME, SpfConfig.CONFIG_HOVER_TIME_DEFAULT));
                    }
                } else {
                    vibratorRun = true;
                    gestureStartTime = 0;
                }

                setPosition(originX + ((touchCurrentX - touchStartX) / animationScaling), originY + ((touchStartY - touchCurrentY) / animationScaling));

                setPressure(event);

                return false;
            }

            private boolean onTouchUp(MotionEvent event) {
                if (!isTouchDown || isGestureCompleted) {
                    return true;
                }

                isTouchDown = false;
                isGestureCompleted = true;
                lastTouchDown = 0L;

                float moveX = event.getX() - touchStartX;
                float moveY = touchStartY - event.getY();

                if (Math.abs(moveX) > flingValue || Math.abs(moveY) > flingValue) {
                    if (moveY > FLIP_DISTANCE) {
                        if (isLongTimeGesture) // 上滑悬停
                            performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_SLIDE_UP_HOVER, SpfConfig.IOS_BAR_SLIDE_UP_HOVER_DEFAULT));
                        else // 上滑
                            performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_SLIDE_UP, SpfConfig.IOS_BAR_SLIDE_UP_DEFAULT));
                    } else if (moveX < -FLIP_DISTANCE) { // 向左滑动
                        performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_SLIDE_LEFT, SpfConfig.IOS_BAR_SLIDE_LEFT_DEFAULT));
                    } else if (moveX > FLIP_DISTANCE) { // 向右滑动
                        performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_SLIDE_RIGHT, SpfConfig.IOS_BAR_SLIDE_RIGHT_DEFAULT));
                    }
                } else {
                    int pressureAction = config.getInt(SpfConfig.IOS_BAR_PRESS, SpfConfig.IOS_BAR_PRESS_DEFAULT);
                    if (pressureAction != SpfConfig.IOS_BAR_TOUCH_DEFAULT && pressureMin != SpfConfig.IOS_BAR_PRESS_MIN_DEFAULT && pressure >= pressureMin) {
                        // 按压
                        performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_PRESS, SpfConfig.IOS_BAR_PRESS_DEFAULT));
                        int action = config.getInt(SpfConfig.IOS_BAR_PRESS, SpfConfig.IOS_BAR_PRESS_DEFAULT);
                        if (action != SpfConfig.IOS_BAR_PRESS_DEFAULT) {
                            touchVibrator(true);
                        }
                    } else {
                        // 轻触
                        performGlobalAction(ActionModel.getConfig(config, SpfConfig.IOS_BAR_TOUCH, SpfConfig.IOS_BAR_TOUCH_DEFAULT));
                        int action = config.getInt(SpfConfig.IOS_BAR_TOUCH, SpfConfig.IOS_BAR_TOUCH_DEFAULT);
                        if (action != SpfConfig.IOS_BAR_TOUCH_DEFAULT) {
                            touchVibrator();
                        }
                    }
                }

                clearEffect();

                return true;
            }

            void clearEffect() {
                pressure = 0;

                animationTo(originX, originY, 800, new OvershootInterpolator());
                // if (isLandscapf) {
                fadeOut();
                // }
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event != null) {
                    String text = "Pressure:" + event.getPressure() + "  size:" + event.getSize();
                    Log.d("MTE", text);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            return onTouchDown(event);
                        }
                        case MotionEvent.ACTION_MOVE: {
                            return onTouchMove(event);
                        }
                        case MotionEvent.ACTION_UP: {
                            return onTouchUp(event);
                        }
                        case MotionEvent.ACTION_CANCEL:
                            clearEffect();
                            return true;
                        case MotionEvent.ACTION_OUTSIDE: {
                            clearEffect();
                            return false;
                        }
                        default: {
                            Log.d("MotionEvent", "com.omarea.gesture OTHER" + event.getAction());
                        }
                    }
                } else {
                    clearEffect();
                }
                return true;
            }

        };
        bar.setOnTouchListener(onTouchListener);
        if (!GlobalState.testMode) {
            bar.setAlpha(fateOutAlpha);
        }

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                ReceiverLock.unRegister(accessibilityService);
                if (config.getBoolean(SpfConfig.IOS_BAR_LOCK_HIDE, SpfConfig.IOS_BAR_LOCK_HIDE_DEFAULT)) {
                    ReceiverLock.autoRegister(accessibilityService, new ReceiverLockHandler(bar, accessibilityService));
                }
                if (config.getBoolean(SpfConfig.IOS_BAR_AUTO_COLOR_ROOT, SpfConfig.IOS_BAR_AUTO_COLOR_ROOT_DEFAULT)) {
                    GlobalState.updateBar = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                bar.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        bar.invalidate();
                                    }
                                });
                            } catch (Exception ex) {
                            }
                        }
                    };
                    ScreenColor.updateBarColor(false);
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
            }
        });
        if (config.getBoolean(SpfConfig.IOS_BAR_LOCK_HIDE, SpfConfig.IOS_BAR_LOCK_HIDE_DEFAULT)) {
            if (new ScreenState(accessibilityService).isScreenLocked()) {
                bar.setVisibility(View.GONE);
            }
        }

        return view;
    }

    private void touchVibrator() {
        touchVibrator(false);
    }

    private void touchVibrator(boolean longTime) {
        if (vibrator == null) {
            vibrator = (Vibrator) (accessibilityService.getSystemService(Context.VIBRATOR_SERVICE));
        }
        if (vibrator.hasVibrator()) {
            vibrator.cancel();
            int time = config.getInt(SpfConfig.VIBRATOR_TIME, SpfConfig.VIBRATOR_TIME_DEFAULT);
            if (longTime) {
                time = (int) (time * 1.5);
            }
            int amplitude = config.getInt(SpfConfig.VIBRATOR_AMPLITUDE, SpfConfig.VIBRATOR_AMPLITUDE_DEFAULT);
            if (time > 0 && amplitude > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(time, amplitude));
                } else {
                    vibrator.vibrate(new long[]{0, time, amplitude}, -1);
                }
            }
        }
    }
}
