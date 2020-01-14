package com.omarea.gesture.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import com.omarea.gesture.AccessibilitySceneGesture;
import com.omarea.gesture.ActionModel;
import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;
import com.omarea.gesture.util.Handlers;

public class ThreeSectionView extends View {
    private SharedPreferences config;
    private ValueAnimator va = null; // 动画程序
    private int bakWidth = 0;
    private int bakHeight = 0;

    private float touchStartX = 0F; // 触摸开始位置
    private float touchStartY = 0F; // 触摸开始位置
    private float touchCurrentX = 0F; // 当前触摸位置
    private float touchCurrentY = 0F; // 当前触摸位置
    private long gestureStartTime = 0L; // 手势开始时间（是指滑动到一定距离，认定触摸手势生效的时间）
    private boolean isLongTimeGesture = false;
    private Context context = getContext();
    private int FLIP_DISTANCE = dp2px(context, 50f); // 触摸灵敏度（滑动多长距离认为是手势）
    private float effectSize = (float) (dp2px(context, 15f)); // 特效大小
    private boolean isTouchDown = false;
    private boolean isGestureCompleted = false;
    private float iconRadius = dp2px(context, 8f);
    private float currentGraphSize = 0f;
    private Vibrator vibrator = (Vibrator) (context.getSystemService(Context.VIBRATOR_SERVICE));
    private boolean vibratorRun = false;
    private float flingValue = dp2px(context, 3f); // 小于此值认为是点击而非滑动

    private ActionModel eventLeftSlide;
    private ActionModel eventLeftHover;
    private ActionModel eventCenterSlide;
    private ActionModel eventCenterHover;
    private ActionModel eventRightSlide;
    private ActionModel eventRightHover;
    private AccessibilitySceneGesture accessibilityService;
    private boolean isLandscapf = false;
    private boolean gameOptimization = false;

    private Paint p = new Paint();
    private long lastEventTime = 0L;
    private int lastEvent = -1;

    private float touchRawX;
    private float touchRawY;

    public ThreeSectionView(Context context) {
        super(context);
        init();
    }

    public ThreeSectionView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public ThreeSectionView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init();
    }

    private void init() {
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setShadowLayer(dp2px(context, 1), 0, 0, 0x99000000);
        p.setStrokeWidth(dp2px(context, 3));

        config = context.getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE);
    }

    private void performGlobalAction(ActionModel event) {
        if (accessibilityService != null) {
            if (gameOptimization && isLandscapf && ((System.currentTimeMillis() - lastEventTime) > 2000 || lastEvent != event.actionCode)) {
                lastEvent = event.actionCode;
                lastEventTime = System.currentTimeMillis();
                Toast.makeText(context, this.getContext().getString(R.string.please_repeat), Toast.LENGTH_SHORT).show();
            } else {
                Handlers.executeVirtualAction(accessibilityService, event, touchRawX, touchRawY);
            }
        }
    }

    private void onShortTouch() {
        if (accessibilityService != null) {
            float p = touchStartX / getWidth();
            if (p > 0.6f) {
                performGlobalAction(eventRightSlide);
            } else if (p > 0.4f) {
                performGlobalAction(eventCenterSlide);
            } else {
                performGlobalAction(eventLeftSlide);
            }
        }
    }

    private void onTouchHover() {
        if (accessibilityService != null) {
            float p = touchStartX / getWidth();
            if (p > 0.6f) {
                performGlobalAction(eventRightHover);
            } else if (p > 0.4f) {
                performGlobalAction(eventCenterHover);
            } else {
                performGlobalAction(eventLeftHover);
            }
        }
    }

    void touchVibrator() {
        if (vibrator.hasVibrator()) {
            vibrator.cancel();
            int time = config.getInt(SpfConfig.VIBRATOR_TIME, SpfConfig.VIBRATOR_TIME_DEFAULT);
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

    void setBarPosition(boolean isLandscapf, boolean gameOptimization, int width, int height) {
        this.isLandscapf = isLandscapf;
        this.gameOptimization = gameOptimization;
        p.setColor(config.getInt(SpfConfig.THREE_SECTION_COLOR, SpfConfig.THREE_SECTION_COLOR_DEFAULT));

        setSize(width, height);
    }

    private void setSize(int width, int height) {
        ViewGroup.LayoutParams lp = this.getLayoutParams();
        int h = height;
        int w = width;
        if (h < 1) {
            h = 1;
        }
        if (w < 1) {
            w = 1;
        }
        lp.width = w;
        lp.height = h;
        this.bakWidth = width;
        this.bakHeight = height;
        this.setLayoutParams(lp);
    }

    void setEventHandler(ActionModel leftSlide, ActionModel leftHover, ActionModel centerSlide, ActionModel centerHover, ActionModel rightSlide, ActionModel rightHover, final AccessibilitySceneGesture context) {
        this.eventLeftSlide = leftSlide;
        this.eventLeftHover = leftHover;
        this.eventCenterSlide = centerSlide;
        this.eventCenterHover = centerHover;
        this.eventRightSlide = rightSlide;
        this.eventRightHover = rightHover;

        this.accessibilityService = context;
    }

    // 动画（触摸效果）显示期间，将悬浮窗显示调大，以便显示完整的动效
    private void setSizeOnTouch() {
        if (bakHeight > 0 || bakWidth > 0) {
            ViewGroup.LayoutParams lp = this.getLayoutParams();
            lp.height = FLIP_DISTANCE;
            // if (isLandscapf) {
            // setBackground(context.getDrawable(R.drawable.landscape_bar_background));
            // }
            this.setLayoutParams(lp);
        }
    }

    // 动画结束后缩小悬浮窗，以免影响正常操作
    private void resumeBackupSize() {
        touchStartX = 0f;
        touchStartY = 0f;

        ViewGroup.LayoutParams lp = this.getLayoutParams();
        lp.height = bakHeight;
        this.setLayoutParams(lp);
        invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null) {
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
                    cleartEffect();
                    return true;
                case MotionEvent.ACTION_OUTSIDE: {
                    cleartEffect();
                    return false;
                }
                default: {
                    Log.d("MotionEvent", "com.omarea.gesture OTHER" + event.getAction());
                }
            }
        } else {
            cleartEffect();
        }
        return true;
    }

    private boolean onTouchDown(MotionEvent event) {
        isTouchDown = true;
        isGestureCompleted = false;
        touchStartX = event.getX();
        touchStartY = event.getRawY();
        gestureStartTime = 0;
        isLongTimeGesture = false;
        currentGraphSize = (float) (dp2px(context, 5f));
        setSizeOnTouch();
        currentGraphSize = 0f;
        vibratorRun = true;

        return true;
    }

    private float getGraphSize() {
        float height = getHeight();

        float posY = (touchCurrentY - touchStartY) / FLIP_DISTANCE / 8;

        if (posY < -0.7f) {
            posY = -0.7f;
        }

        return (height * (1 + posY));
    }

    private boolean onTouchMove(MotionEvent event) {
        if (isGestureCompleted || !isTouchDown) {
            return true;
        }

        touchRawX = event.getRawX();
        touchRawY = event.getRawY();

        touchCurrentX = event.getX();
        touchCurrentY = event.getRawY();
        float a = touchStartY;
        float b = touchCurrentY;

        if (a - b > FLIP_DISTANCE) {
            if (gestureStartTime < 1) {
                final long currentTime = System.currentTimeMillis();
                gestureStartTime = currentTime;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isTouchDown && !isGestureCompleted && currentTime == gestureStartTime) {
                            isLongTimeGesture = true;
                            if (vibratorRun) {
                                touchVibrator();
                                vibratorRun = false;
                            }
                            onTouchHover();
                            isGestureCompleted = true;
                            cleartEffect();
                        }
                    }
                }, config.getInt(SpfConfig.CONFIG_HOVER_TIME, SpfConfig.CONFIG_HOVER_TIME_DEFAULT));
            }
        } else {
            vibratorRun = true;
            gestureStartTime = 0;
        }

        currentGraphSize = getGraphSize();
        invalidate();
        return true;
    }

    private boolean onTouchUp(MotionEvent event) {
        if (!isTouchDown || isGestureCompleted) {
            return true;
        }

        isTouchDown = false;
        isGestureCompleted = true;

        float moveX = event.getX() - touchStartX;
        float moveY = touchStartY - event.getRawY();

        if (Math.abs(moveX) > flingValue || Math.abs(moveY) > flingValue) {
            if (moveY > FLIP_DISTANCE) { // 纵向滑动
                if (isLongTimeGesture)
                    onTouchHover();
                else
                    onShortTouch();
            } else if (moveX > FLIP_DISTANCE) { // 横向滑动

            }
        }
        cleartEffect();

        return true;
    }

    /**
     * dp转换成px
     */
    private int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 清除手势效果
     */
    private void cleartEffect() {
        invalidate();
        isTouchDown = false;

        if (va != null && va.isRunning()) {
            va.cancel();
        }
        final float viewHeight = getHeight();
        va = ValueAnimator.ofFloat(this.currentGraphSize, viewHeight);

        va.setDuration(200);
        va.setInterpolator(new AccelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                currentGraphSize = (float) (animation.getAnimatedValue());
                if (touchCurrentX > 0 || touchCurrentY > 0) {
                    if (currentGraphSize < iconRadius) {
                        touchCurrentX = 0f;
                        touchCurrentY = 0f;
                        gestureStartTime = 0;
                    }
                }

                if (currentGraphSize >= viewHeight && !isTouchDown) {
                    resumeBackupSize();
                }
                invalidate();
            }
        });
        va.start();
        if (isLandscapf) {
            setBackground(null);
        }
    }

    @Override
    @SuppressLint("DrawAllocation")
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pos = touchStartX / getWidth();

        if (currentGraphSize > 0 && touchStartY > 0f) {
            if (pos > 0.6f) {
                canvas.drawLine(bakWidth * 0.65f, currentGraphSize, bakWidth * 0.95f, currentGraphSize, p);
                if (isLandscapf) {
                    float y = getHeight() - p.getStrokeWidth();
                    canvas.drawLine(bakWidth * 0.37f, y, bakWidth * 0.63f, y, p);
                    canvas.drawLine(bakWidth * 0.05f, y, bakWidth * 0.33f, y, p);
                }
            } else if (pos > 0.4f) {
                canvas.drawLine(bakWidth * 0.35f, currentGraphSize, bakWidth * 0.65f, currentGraphSize, p);
                if (isLandscapf) {
                    float y = getHeight() - p.getStrokeWidth();
                    canvas.drawLine(bakWidth * 0.67f, y, bakWidth * 0.95f, y, p);
                    canvas.drawLine(bakWidth * 0.05f, y, bakWidth * 0.33f, y, p);
                }
            } else {
                canvas.drawLine(bakWidth * 0.05f, currentGraphSize, bakWidth * 0.35f, currentGraphSize, p);
                if (isLandscapf) {
                    float y = getHeight() - p.getStrokeWidth();
                    canvas.drawLine(bakWidth * 0.67f, y, bakWidth * 0.95f, y, p);
                    canvas.drawLine(bakWidth * 0.37f, y, bakWidth * 0.63f, y, p);
                }
            }
        } else {
            // canvas.drawLine(bakWidth * 0.66f, getHeight() - 10, bakWidth * 0.95f, getHeight() - 10, p);
            // canvas.drawLine(bakWidth * 0.36f, getHeight() - 10, bakWidth * 0.64f, getHeight() - 10, p);
            // canvas.drawLine(bakWidth * 0.06f, getHeight() - 10, bakWidth * 0.34f, getHeight() - 10, p);
        }
    }
}
