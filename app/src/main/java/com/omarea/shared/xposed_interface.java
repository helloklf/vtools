package com.omarea.shared;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.setIntField;

/**
 * Created by helloklf on 2016/10/1.
 */
public class xposed_interface implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    //IXposedHookInitPackageResources
    //public static ServiceHelper serviceHelper;
    private static XSharedPreferences prefs;
    private static XSharedPreferences prefs2;
    private boolean useDefaultConfig = false;

    private int dpiCompute(Context context) {
        float w = context.getResources().getDisplayMetrics().widthPixels;
        float h = context.getResources().getDisplayMetrics().heightPixels;
        float pixels = w > h ? h : w;

        /*if (pixels == 2160) {
            context.getResources().getDisplayMetrics().density = 6;
            context.getResources().getDisplayMetrics().densityDpi = 960;
            context.getResources().getDisplayMetrics().scaledDensity = 6;
        } else if (pixels == 1440) {
            context.getResources().getDisplayMetrics().density = 4;
            context.getResources().getDisplayMetrics().densityDpi = 640;
            context.getResources().getDisplayMetrics().scaledDensity = 4;
        } else if (pixels == 1080) {
            context.getResources().getDisplayMetrics().density = 3;
            context.getResources().getDisplayMetrics().densityDpi = 480;
            context.getResources().getDisplayMetrics().scaledDensity = 3;
        } else if (pixels == 720) {
            context.getResources().getDisplayMetrics().density = 2;
            context.getResources().getDisplayMetrics().densityDpi = 320;
            context.getResources().getDisplayMetrics().scaledDensity = 2;
        } else if (pixels == 540 || pixels == 480) {
            context.getResources().getDisplayMetrics().density = (float) (1.5);
            context.getResources().getDisplayMetrics().densityDpi = 240;
            context.getResources().getDisplayMetrics().scaledDensity = (float) (1.5);
        }*/
        return (int) (pixels * 2.25);
    }

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences("com.omarea.vboot", "xposed");
        prefs2 = new XSharedPreferences("com.omarea.vboot", "xposed_dpifix");
        //prefs.reload();
        //强制绕开权限限制读取配置 因为SharedPreferences在Android N中不能设置为MODE_WORLD_READABLE
        prefs.makeWorldReadable();
        prefs2.makeWorldReadable();
        //useDefaultConfig = prefs.getAll().size() == 0;
        final int defaultDpi = prefs.getInt("xposed_default_dpi", 480);

        XposedHelpers.findAndHookMethod(DisplayMetrics.class, "getDeviceDensity", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                param.setResult(defaultDpi);
            }
        });

        XposedHelpers.findAndHookMethod(Display.class, "updateDisplayInfoLocked", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = AndroidAppHelper.currentPackageName();
                if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && prefs2.contains(packageName)) {
                    Object mDisplayInfo = XposedHelpers.getObjectField(param.thisObject, "mDisplayInfo");
                    XposedHelpers.setIntField(mDisplayInfo, "logicalDensityDpi", defaultDpi);
                }
            }

            ;
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
                            if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && (prefs2.contains(packageName) || prefs2.contains(hostPackageName))) {
                                dpi = defaultDpi;
                            } else if (packageName.equals("com.waterdaaan.cpufloat") || packageName.equals("eu.chainfire.perfmon")) {
                                dpi = defaultDpi / 1.7f;
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
                                newMetrics.densityDpi = (int)dpi;
                                newMetrics.scaledDensity = dpi / 160f;

                                if (Build.VERSION.SDK_INT >= 17)
                                    setIntField(newConfig, "densityDpi", (int)dpi);
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
        final String packageName = loadPackageParam.packageName;
        prefs.reload();
        prefs2.reload();
        final int defaultDpi = prefs.getInt("xposed_default_dpi", 480);

        if (packageName.equals("com.android.systemui")) {
            if (useDefaultConfig || prefs.getBoolean("xposed_hide_su", false)) {
                //隐藏cm状态栏su图标
                XposedBridge.hookAllMethods(
                        XposedHelpers.findClass(
                                "com.android.systemui.statusbar.phone.PhoneStatusBarPolicy",
                                loadPackageParam.classLoader
                        ),
                        "updateSu",
                        new XC_MethodHook() {
                            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                param.setResult(0x0);
                            }
                        });
            }
        }

        //用于检查xposed是否激活
        else if (packageName.equals("com.omarea.vboot")) {
            XposedHelpers.findAndHookMethod("com.omarea.shared.xposed_check", loadPackageParam.classLoader, "xposedIsRunning", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    param.setResult(true);
                }
            });
        }

        //王者荣耀 高帧率模式
        else if (packageName.equals("com.tencent.tmgp.sgame")) {
            if (useDefaultConfig || prefs.getBoolean("xposed_hight_fps", false)) {
                XposedHelpers.findAndHookMethod(
                        "android.os.SystemProperties", loadPackageParam.classLoader, "get", String.class, String.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                XposedHelpers.setStaticObjectField(android.os.Build.class, "PRODUCT", "R11 Plus");
                                XposedHelpers.setStaticObjectField(android.os.Build.class, "DEVICE", "R11 Plus");
                                XposedHelpers.setStaticObjectField(android.os.Build.class, "MODEL", "OPPO R11 Plus");
                                XposedHelpers.setStaticObjectField(android.os.Build.class, "BRAND", "OPPO");
                                XposedHelpers.setStaticObjectField(android.os.Build.class, "MANUFACTURER", "OPPO");
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                String prop = (String) param.args[0];
                                switch (prop) {
                                    case "ro.product.model": {
                                        param.setResult("OPPO R11 Plus");
                                        break;
                                    }
                                    case "ro.product.brand": {
                                        param.setResult("OPPO");
                                        break;
                                    }
                                    case "ro.product.manufacturer": {
                                        param.setResult("OPPO");
                                        break;
                                    }
                                    case "ro.product.device": {
                                        param.setResult("R11 Plus");
                                        break;
                                    }
                                    case "ro.product.name": {
                                        param.setResult("R11 Plus");
                                        break;
                                    }
                                    default: {
                                        break;
                                    }
                                }
                            }
                        });
            }
        } else if (packageName.equals("com.waterdaaan.cpufloat") || packageName.equals("eu.chainfire.perfmon")) {
            if (useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) {
                XposedHelpers.findAndHookMethod("android.app.Application", loadPackageParam.classLoader, "attach", Context.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);

                        Context context = (Context) param.args[0];
                        context.getResources().getDisplayMetrics().density = (float) (context.getResources().getDisplayMetrics().density / 1.7);
                        context.getResources().getDisplayMetrics().densityDpi = (int) (context.getResources().getDisplayMetrics().densityDpi / 1.7);
                        context.getResources().getDisplayMetrics().scaledDensity = (float) (context.getResources().getDisplayMetrics().scaledDensity / 1.7);
                    }
                });
            }
        } else if (packageName.equals("net.oneplus.launcher")) {
            if (useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) {
                XposedHelpers.findAndHookMethod("android.app.Application", loadPackageParam.classLoader, "attach", Context.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);

                        Context context = (Context) param.args[0];
                        context.getResources().getDisplayMetrics().density = (float) (defaultDpi * 0.9);
                        context.getResources().getDisplayMetrics().densityDpi = (int) (defaultDpi * 0.9);
                        context.getResources().getDisplayMetrics().scaledDensity = (float) (defaultDpi * 0.9);
                    }
                });
            }
        }

        if (useDefaultConfig || prefs.getBoolean("xposed_webview_debug", false)) {
            //强制开启webview调试
            XposedBridge.hookAllConstructors(android.webkit.WebView.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.callStaticMethod(android.webkit.WebView.class, "setWebContentsDebuggingEnabled", true);
                }
            });

            XposedBridge.hookAllMethods(android.webkit.WebView.class, "setWebContentsDebuggingEnabled", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });
        }
        if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && (prefs2.contains(packageName))) {
            XposedHelpers.findAndHookMethod("android.app.Application", loadPackageParam.classLoader, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    Context context = (Context) param.args[0];
                    context.getResources().getDisplayMetrics().density = defaultDpi / 160.0f;
                    context.getResources().getDisplayMetrics().densityDpi = defaultDpi;
                    context.getResources().getDisplayMetrics().scaledDensity = defaultDpi / 160.0f;
                }
            });
        }
        /*
        float dpi = getApplicationContext().getResources().getDisplayMetrics().density;
        getApplicationContext().getResources().getDisplayMetrics().density = 2;
        Toast.makeText(getApplicationContext(),String.valueOf(dpi),Toast.LENGTH_SHORT).show();
        */
    }
}
