package com.omarea.xposed;

import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;

import androidx.recyclerview.widget.RecyclerView;

import com.omarea.library.calculator.Flags;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class ReverseOptimizer {
    // 设置滚动缓存
    private void hookRecyclerViewCache  () {
        XposedBridge.hookAllConstructors(RecyclerView.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                callMethod(param.thisObject, "setItemViewCacheSize", 0);
            }
        });
        XposedHelpers.findAndHookMethod(
                RecyclerView.class,
                "setItemViewCacheSize",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[0] = 0;
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    }
                });
    }

    // 禁用硬件加速
    private void hookHardwareAccelerated() {
        XposedBridge.hookAllConstructors(Window.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setObjectField(param.thisObject, "mHardwareAccelerated", false);
            }
        });
        XposedHelpers.findAndHookMethod(Window.class, "setWindowManager", WindowManager.class, IBinder.class, String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[3] = false;
            }
        });
        /*
        // 验证关闭硬件加速的效果
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = ((Activity)param.thisObject);
                boolean isHardwareAccelerated = activity.getWindow().getDecorView().isHardwareAccelerated();
                Toast.makeText(activity, "HardwareAccelerated -> " + isHardwareAccelerated, Toast.LENGTH_SHORT).show();
            }
        });
        */
        XposedHelpers.findAndHookMethod(Window.class, "setFlags", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int flags = new Flags((int) param.args[0]).removeFlag(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
                param.args[0] = flags;
            }
        });
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        hookRecyclerViewCache();
        hookHardwareAccelerated();
    }
}
