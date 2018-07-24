package com.omarea.xposed;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.setIntField;

/**
 * Created by helloklf on 2016/10/1.
 */
public class XposedInterface implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static XSharedPreferences prefs;
    private boolean useDefaultConfig = false;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences("com.omarea.vaddin", "xposed");

        //prefs.reload();
        //强制绕开权限限制读取配置 因为SharedPreferences在Android N中不能设置为MODE_WORLD_READABLE
        prefs.makeWorldReadable();
        //useDefaultConfig = prefs.getAll().size() == 0;
        final int defaultDpi = prefs.getInt("xposed_default_dpi", 480);

        XposedHelpers.findAndHookMethod(DisplayMetrics.class, "getDeviceDensity", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                String packageName = AndroidAppHelper.currentPackageName();
                String key = packageName + "_dpi";
                if (prefs.contains(key)) {
                    param.setResult(prefs.getInt(key, defaultDpi));
                }
            }
        });

        XposedHelpers.findAndHookMethod(Display.class, "updateDisplayInfoLocked", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = AndroidAppHelper.currentPackageName();
                String key = packageName + "_dpi";
                if (prefs.contains(key)) {
                    Object mDisplayInfo = XposedHelpers.getObjectField(param.thisObject, "mDisplayInfo");
                    XposedHelpers.setIntField(mDisplayInfo, "logicalDensityDpi", prefs.getInt(key, defaultDpi));
                }
            }
        });
        XposedHelpers.findAndHookMethod(Display.class, "getMetrics", DisplayMetrics.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = AndroidAppHelper.currentPackageName();
                String key = packageName + "_dpi";
                if (prefs.contains(key)) {
                    Object mDisplayInfo = XposedHelpers.getObjectField(param.thisObject, "mDisplayInfo");
                    int dpi = prefs.getInt(key, defaultDpi);
                    XposedHelpers.setIntField(mDisplayInfo, "logicalDensityDpi", dpi);
                    DisplayMetrics displayMetrics = (DisplayMetrics) param.args[0];
                    displayMetrics.scaledDensity = dpi / 160.0f;
                    displayMetrics.densityDpi = dpi;
                    displayMetrics.density = dpi / 160.0f;
                }
            }
        });

        try {
            findAndHookMethod(Resources.class, "updateConfiguration",
                    Configuration.class, DisplayMetrics.class, "android.content.res.CompatibilityInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0] == null)
                                return;
                            String packageName;
                            Resources res = ((Resources) param.thisObject);
                            if (res instanceof XResources) {
                                packageName = ((XResources) res).getPackageName();
                            } else if (res instanceof XModuleResources) {
                                return;
                            } else {
                                try {
                                    packageName = XResources.getPackageNameDuringConstruction();
                                } catch (IllegalStateException e) {
                                    // That's ok, we might have been called for
                                    // non-standard resources
                                    return;
                                }
                            }
                            String hostPackageName = AndroidAppHelper.currentPackageName();
                            float dpi = 0;
                            String key = packageName + "_dpi";
                            String key2 = hostPackageName + "_dpi";
                            if ((prefs.contains(key) || prefs.contains(key2))) {
                                dpi = prefs.getInt(key, prefs.getInt(key2, defaultDpi));
                            } else {
                                return;
                            }
                            Configuration newConfig = null;
                            newConfig = new Configuration((Configuration) param.args[0]);

                            DisplayMetrics newMetrics;
                            if (param.args[1] != null) {
                                newMetrics = (DisplayMetrics) param.args[1];
                            } else {
                                newMetrics = res.getDisplayMetrics();
                            }

                            if (dpi > 0) {
                                newMetrics.density = dpi / 160f;
                                newMetrics.densityDpi = (int) dpi;
                                newMetrics.scaledDensity = dpi / 160f;
                                if (Build.VERSION.SDK_INT >= 17) {
                                    setIntField(newConfig, "densityDpi", (int) dpi);
                                }
                            }

                            if (newConfig != null)
                                param.args[0] = newConfig;
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (prefs.getBoolean("xposed_full_screen", true)) {
            new FullScreeProcess().addMarginBottom();
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        final String packageName = loadPackageParam.packageName;

        //平滑滚动
        if (prefs.getBoolean(packageName + "_scroll", false)) {
            new ViewConfig().handleLoadPackage(loadPackageParam);
        }

        final int defaultDpi = prefs.getInt("xposed_default_dpi", 480);

        switch (packageName) {
            case "com.android.systemui":
                if (prefs.getBoolean("xposed_hide_su", false)) {
                    new SystemUI().hideSUIcon(loadPackageParam);
                }
                break;

            //用于检查xposed是否激活
            case "com.omarea.vtools":
                //case "com.omarea.vaddin":
                new ActiveCheck().isActive(loadPackageParam);
                break;

            //王者荣耀 高帧率模式
            case "com.tencent.tmgp.sgame":
                if (prefs.getBoolean("xposed_hight_fps", false)) {
                    new DeviceInfo().simulationR11(loadPackageParam);
                }
                break;
        }
        if (prefs.getBoolean(packageName + "_webdebug", false)) {
            new WebView().allowDebug();
        }
        if (prefs.getBoolean(packageName + "_hide_recent", false)) {
            XposedHelpers.findAndHookMethod("android.app.Activity", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Activity activity = (Activity) param.thisObject;
                    if (activity != null) {
                        ActivityManager service = (ActivityManager) (activity.getSystemService(Context.ACTIVITY_SERVICE));
                        if (service == null)
                            return;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            for (ActivityManager.AppTask task : service.getAppTasks()) {
                                if (task.getTaskInfo().id == activity.getTaskId()) {
                                    task.setExcludeFromRecents(true);
                                }
                            }
                        } else {
                            //TODO：隐藏最近任务，暂不支持5.0以下
                        }
                    }
                }
            });
        }
        final String keyDPI = packageName + "_dpi";
        if (prefs.getInt(keyDPI, 0) >= 96) {
            XposedHelpers.findAndHookMethod("android.app.Application", loadPackageParam.classLoader, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    prefs.reload();

                    if (prefs.getInt(keyDPI, 0) > 0) {
                        Context context = (Context) param.args[0];
                        context.getResources().getDisplayMetrics().density = prefs.getInt(keyDPI, defaultDpi) / 160.0f;
                        context.getResources().getDisplayMetrics().densityDpi = prefs.getInt(keyDPI, defaultDpi);
                        context.getResources().getDisplayMetrics().scaledDensity = prefs.getInt(keyDPI, defaultDpi) / 160.0f;
                    }
                }
            });

            XposedHelpers.findAndHookMethod("android.util.DisplayMetrics", loadPackageParam.classLoader, "setTo", DisplayMetrics.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if (prefs.contains(keyDPI)) {
                        DisplayMetrics displayMetrics = (DisplayMetrics) (param.args[0]);
                        if (displayMetrics != null) {
                            int dpi = prefs.getInt(keyDPI, defaultDpi);
                            displayMetrics.density = dpi / 160.0f;
                            displayMetrics.densityDpi = dpi;
                            displayMetrics.scaledDensity = dpi / 160.0f;
                        }
                    }
                }
            });
            XposedHelpers.findAndHookMethod("android.util.DisplayMetrics", loadPackageParam.classLoader, "getRealMetrics", DisplayMetrics.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if (prefs.contains(keyDPI)) {
                        DisplayMetrics displayMetrics = (DisplayMetrics) (param.args[0]);
                        if (displayMetrics != null) {
                            int dpi = prefs.getInt(keyDPI, defaultDpi);
                            displayMetrics.density = dpi / 160.0f;
                            displayMetrics.densityDpi = dpi;
                            displayMetrics.scaledDensity = dpi / 160.0f;
                        }
                    }
                }
            });
        }
    }
}
