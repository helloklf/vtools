package com.omarea.xposed;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AndroidAppHelper;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
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

import static de.robv.android.xposed.XposedHelpers.callMethod;

/**
 * Created by helloklf on 2016/10/1.
 */
public class XposedInterface implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static XSharedPreferences prefs;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences("com.omarea.vaddin", "xposed");

        //强制绕开权限限制读取配置 因为SharedPreferences在Android N中不能设置为MODE_WORLD_READABLE
        prefs.makeWorldReadable();
        final boolean disServiceForeground = prefs.getBoolean("android_dis_service_foreground", false);

        XposedHelpers.findAndHookMethod(Service.class, "setForeground", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (disServiceForeground) {
                    XposedBridge.log("禁止前台模式，hook setForeground in" + AndroidAppHelper.currentPackageName());
                    param.args[0] = false;
                }
                super.afterHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                try {
                    XposedBridge.log("param.args[0]：" + param.args[0]);
                    if ((Boolean) param.args[0]) {
                        callMethod(param.thisObject, "setForeground", false);
                    }
                } catch (Exception ignored) {
                }
            }
        });
        XposedHelpers.findAndHookMethod(Service.class, "startForeground", int.class, Notification.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (disServiceForeground) {
                    callMethod(param.thisObject, "stopForeground", true);
                    /*
                    try {
                        Service service = (Service) param.thisObject;
                        NotificationManager notificationManager = (NotificationManager) service.getSystemService(Service.NOTIFICATION_SERVICE);
                        notificationManager.cancel((Integer) param.args[0]);
                    } catch (Exception ex) {

                    }
                    */
                    XposedBridge.log("禁止前台模式，hook startForeground in " + AndroidAppHelper.currentPackageName());
                }
            }
        });

        XposedHelpers.findAndHookMethod(DisplayMetrics.class, "getDeviceDensity", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                String key = AndroidAppHelper.currentPackageName() + "_dpi";
                if (prefs.contains(key)) {
                    int dpi = prefs.getInt(key, 0);
                    if (dpi < 96)
                        return;
                    param.setResult(dpi);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Display.class, "updateDisplayInfoLocked", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String key = AndroidAppHelper.currentPackageName() + "_dpi";
                if (prefs.contains(key)) {
                    Object mDisplayInfo = XposedHelpers.getObjectField(param.thisObject, "mDisplayInfo");
                    int dpi = prefs.getInt(key, 0);
                    if (dpi < 96)
                        return;
                    XposedHelpers.setIntField(mDisplayInfo, "logicalDensityDpi", dpi);
                }
            }
        });
        XposedHelpers.findAndHookMethod(Display.class, "getMetrics", DisplayMetrics.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String key = AndroidAppHelper.currentPackageName() + "_dpi";
                if (prefs.contains(key)) {
                    int dpi = prefs.getInt(key, 0);
                    if (dpi < 96)
                        return;
                    Object mDisplayInfo = XposedHelpers.getObjectField(param.thisObject, "mDisplayInfo");
                    XposedHelpers.setIntField(mDisplayInfo, "logicalDensityDpi", dpi);
                    DisplayMetrics displayMetrics = (DisplayMetrics) param.args[0];
                    displayMetrics.scaledDensity = dpi / 160.0f;
                    displayMetrics.densityDpi = dpi;
                    displayMetrics.density = dpi / 160.0f;
                }
            }
        });

        try {
            XposedHelpers.findAndHookMethod(Resources.class, "updateConfiguration",
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
                                    return;
                                }
                            }
                            String hostPackageName = AndroidAppHelper.currentPackageName();
                            float dpi = 0;
                            String key = packageName + "_dpi";
                            String key2 = hostPackageName + "_dpi";
                            if ((prefs.contains(key) || prefs.contains(key2))) {
                                dpi = prefs.getInt(key, prefs.getInt(key2, 0));
                            } else {
                                return;
                            }
                            if (dpi < 96) {
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
                                XposedHelpers.setIntField(newConfig, "densityDpi", (int) dpi);
                            }

                            if (newConfig != null)
                                param.args[0] = newConfig;
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.isFirstApplication) {
            return;
        }
        prefs.reload();
        final String packageName = loadPackageParam.packageName;

        // 平滑滚动
        if (prefs.getBoolean(packageName + "_scroll", false) || prefs.getBoolean("android_scroll", false)) {
            if (!packageName.equals("android") && !packageName.equals("com.android.systemui")) {
                new ViewConfig().handleLoadPackage(loadPackageParam);
            }
        }

        // 从最近任务列表隐藏
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

        // 专属选项
        switch (packageName) {
            // 隐藏状态栏ROOT图标
            case "com.android.systemui":
                if (prefs.getBoolean("com.android.systemui_hide_su", false)) {
                    new SystemUI().hideSUIcon(loadPackageParam);
                }
                /*
                // 修改状态栏时间
                XposedHelpers.findAndHookMethod(
                        "com.android.systemui.statusbar.policy.Clock", loadPackageParam.classLoader, "updateClock", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param)
                                    throws Throwable {
                                System.out.println("replaceHookedMethod---updateClock");
                                XposedBridge.log("replaceHookedMethod---updateClock-->>");
                                TextView tv = (TextView) param.thisObject;
                                String text = tv.getText().toString();
                                tv.setText(text + " :)-->");
                                tv.setTextColor(Color.RED);
                            }
                        });
                */
                break;

            // 用于检查xposed是否激活
            case "com.omarea.vtools":
                new ActiveCheck().isActive(loadPackageParam);
                break;
            case "com.omarea.vboot":
                new ActiveCheck().isActive(loadPackageParam);
                break;

            //王者荣耀 高帧率模式
            case "com.tencent.tmgp.sgame":
                if (prefs.getBoolean(packageName + "_hight_fps", false)) {
                    new DeviceInfo().simulationR11(loadPackageParam);
                }
                break;
        }

        // WebView 调试（全局）
        if (prefs.getBoolean("android_webdebug", false)) {
            new WebView().allowDebug();
        }

        // DPI
        final String keyDPI = packageName + "_dpi";
        final int dpi = prefs.getInt(keyDPI, 0);
        if (dpi >= 96) {
            XposedHelpers.findAndHookMethod("android.app.Application", loadPackageParam.classLoader, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    Context context = (Context) param.args[0];
                    if (context == null)
                        return;

                    try {
                        Configuration origConfig = context.getResources().getConfiguration();
                        origConfig.densityDpi = dpi;//获取手机出厂时默认的densityDpi
                        context.getResources().updateConfiguration(origConfig, context.getResources().getDisplayMetrics());
                        context.getResources().getDisplayMetrics().density = dpi / 160.0f;
                        context.getResources().getDisplayMetrics().densityDpi = dpi;
                        context.getResources().getDisplayMetrics().scaledDensity = dpi / 160.0f;
                    } catch (Exception ex) {
                        XposedBridge.log(ex);
                    }
                }
            });

            /*
            XposedHelpers.findAndHookMethod("android.util.DisplayMetrics", loadPackageParam.classLoader, "setTo", DisplayMetrics.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    DisplayMetrics displayMetrics = (DisplayMetrics) (param.args[0]);
                    if (displayMetrics != null) {
                        displayMetrics.density = dpi / 160.0f;
                        displayMetrics.densityDpi = dpi;
                        displayMetrics.scaledDensity = dpi / 160.0f;
                    }
                }
            });
            XposedHelpers.findAndHookMethod("android.util.DisplayMetrics", loadPackageParam.classLoader, "getRealMetrics", DisplayMetrics.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    DisplayMetrics displayMetrics = (DisplayMetrics) (param.args[0]);
                    if (displayMetrics != null) {
                        displayMetrics.density = dpi / 160.0f;
                        displayMetrics.densityDpi = dpi;
                        displayMetrics.scaledDensity = dpi / 160.0f;
                    }
                }
            });
            */
        }
    }
}
