package com.omarea.xposed;

import android.os.Build;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class InDisplayCutoutMode {
    public void forceRoutineDisplay(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        WindowManager.LayoutParams params = (WindowManager.LayoutParams) param.thisObject;
                        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    }
                } catch (Exception ex) {
                    XposedBridge.log("InDisplayCutoutMode: " + ex.getMessage());
                }
                super.afterHookedMethod(param);
            }
        };

        // XposedHelpers.findAndHookConstructor(android.view.WindowManager.LayoutParams.class, hook);
        // XposedHelpers.findAndHookConstructor(android.view.WindowManager.LayoutParams.class, int.class, hook);
        // XposedHelpers.findAndHookConstructor(android.view.WindowManager.LayoutParams.class, int.class, int.class, hook);
        // XposedHelpers.findAndHookConstructor(android.view.WindowManager.LayoutParams.class, int.class, int.class, int.class, hook);
        // XposedHelpers.findAndHookConstructor(android.view.WindowManager.LayoutParams.class, int.class, int.class, int.class, int.class, hook);
        // XposedHelpers.findAndHookConstructor(android.view.WindowManager.LayoutParams.class, int.class, int.class, int.class, int.class, int.class, hook);
        // XposedHelpers.findAndHookConstructor(android.view.WindowManager.LayoutParams.class, int.class, int.class, int.class, int.class, int.class, int.class, hook);
        // XposedHelpers.findAndHookConstructor(android.view.WindowManager.LayoutParams.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, hook);

        XposedBridge.hookAllConstructors(android.view.WindowManager.LayoutParams.class, hook);
    }
}
