package com.omarea.xposed;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewConfiguration;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Hello on 2018/03/02.
 */

public class ViewConfig {
    static final float MULTIPLIER_SCROLL_FRICTION = 10000f;
    static float mDensity = -1;

    private static void hookViewConfiguration(final Class<?> clazz) {
        XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args == null) return;
                // Not null means there's a context for density scaling
                Context context = (Context) param.args[0];
                final Resources res = context.getResources();
                final float density = res.getDisplayMetrics().density;
                if (res.getConfiguration().isLayoutSizeAtLeast(
                        Configuration.SCREENLAYOUT_SIZE_XLARGE)) {
                    mDensity = density * 1.5f;
                } else {
                    mDensity = density;
                }
            }
        });
    }

    private static void hookMaxFlingVelocity(final Class<?> clazz) {
        XposedBridge.hookAllMethods(clazz, "getMaximumFlingVelocity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!isEnabled()) return;
                param.setResult(Common.DEFAULT_SCROLLING_VELOCITY);
            }
        });

        XposedBridge.hookAllMethods(clazz, "getScaledMinimumFlingVelocity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(5);
            }
        });
        XposedBridge.hookAllMethods(clazz, "getScaledMaximumFlingVelocity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!isEnabled()) return;
                final int max_velocity = Common.DEFAULT_SCROLLING_VELOCITY;
                if (mDensity == -1) {
                    param.setResult(max_velocity);
                } else {
                    final int scaled_velocity = (int) (mDensity * max_velocity + 0.5f);
                    param.setResult(scaled_velocity);
                }
            }
        });
    }

    private static void hookScrollFriction(final Class<?> clazz) {
        XposedBridge.hookAllMethods(clazz, "getScrollFriction", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!isEnabled()) return;
                final int raw_friction = Common.DEFAULT_SCROLLING_FRICTION;
                final float actual_friction = ((float) raw_friction) / MULTIPLIER_SCROLL_FRICTION;
                param.setResult(actual_friction);
            }
        });
    }

    private static void hookOverscrollDistance(final Class<?> clazz) {
        XposedBridge.hookAllMethods(clazz, "getScaledOverscrollDistance", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!isEnabled()) return;
                final int overscroll_distance = Common.DEFAULT_SCROLLING_OVERSCROLL;
                if (mDensity == -1) {
                    param.setResult(overscroll_distance);
                } else {
                    final int scaled_dist = (int) (mDensity * overscroll_distance + 0.5f);
                    param.setResult(scaled_dist);
                }
            }
        });
    }

    private static void hookOverflingDistance(final Class<?> clazz) {
        XposedBridge.hookAllMethods(clazz, "getScaledOverflingDistance", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!isEnabled()) return;
                final int overfling = Common.DEFAULT_SCROLLING_OVERFLING;
                if (mDensity == -1) {
                    param.setResult(overfling);
                } else {
                    final int scaled_dist = (int) (mDensity * overfling + 0.5f);
                    param.setResult(scaled_dist);
                }
            }
        });
    }

    private static void hookLongPressTimeout(final Class<?> clazz) {
        XposedBridge.hookAllMethods(View.class, "getLongPressTimeout", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(200);
            }
        });
    }

    private static void hookTapTimeout(final Class<?> clazz) {
        XposedBridge.hookAllMethods(View.class, "getTapTimeout", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(100);
            }
        });
    }

    private static void hookScaledTouchSlop(final Class<?> clazz) {
        XposedBridge.hookAllMethods(View.class, "getScaledTouchSlop", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(4);
            }
        });
    }

    private static boolean isEnabled() {
        return Common.DEFAULT_SCROLLING_ENABLE;
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        hookViewConfiguration(ViewConfiguration.class);
        hookOverscrollDistance(ViewConfiguration.class);
        hookOverflingDistance(ViewConfiguration.class);
        hookMaxFlingVelocity(ViewConfiguration.class);
        hookScrollFriction(ViewConfiguration.class);
        hookLongPressTimeout(ViewConfiguration.class);
        hookTapTimeout(ViewConfiguration.class);
        hookScaledTouchSlop(ViewConfiguration.class);

        /*
        XposedBridge.hookAllMethods(ViewConfiguration.class, "getScaledMaximumDrawingCacheSize",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(0);
                    }
                }
        );
        */


        // TODO velocity 0 to 100000 / def 8000 // try 2000
        // overscroll dist 0 to 1000 / def 0
        // overfling dist 0 to 1000 / def 6
        // friction * 10000 // 0 to 2000 //def 150 // try 50

        //overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent)
        /*
        XposedBridge.hookAllMethods(View.class, "overScrollBy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (param.args.length == 9) {
                    //mMaxOverScrollY
                    param.args[6] = 200;
                    param.args[7] = 400;
                }
            }
        });
        XposedHelpers.findAndHookMethod(View.class, "overScrollBy",
                int.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,boolean.class , new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                //ListView
            }
        });
        */
    }

    public class Common {
        //是否启用
        static final boolean DEFAULT_SCROLLING_ENABLE = true;
        //滚动溢出最大距离
        static final int DEFAULT_SCROLLING_OVERSCROLL = 150;
        //滚动回弹距离
        static final int DEFAULT_SCROLLING_OVERFLING = 150;
        //滚动惯性
        static final int DEFAULT_SCROLLING_FRICTION = 120;
        //最大滚动速度
        static final int DEFAULT_SCROLLING_VELOCITY = 3000;
    }
}
