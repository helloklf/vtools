package com.omarea.shared;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;

import com.omarea.xposed.ActiveCheck;
import com.omarea.xposed.DeviceInfo;
import com.omarea.xposed.SystemUI;
import com.omarea.xposed.WebView;

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
        prefs2 = new XSharedPreferences("com.omarea.vboot", SpfConfig.XPOSED_DPI_SPF);

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
                String packageName = AndroidAppHelper.currentPackageName();
                if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && prefs2.contains(packageName)) {
                    param.setResult(prefs2.getInt(packageName, defaultDpi));
                }
            }
        });

        XposedHelpers.findAndHookMethod(Display.class, "updateDisplayInfoLocked", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = AndroidAppHelper.currentPackageName();
                if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && prefs2.contains(packageName)) {
                    Object mDisplayInfo = XposedHelpers.getObjectField(param.thisObject, "mDisplayInfo");
                    XposedHelpers.setIntField(mDisplayInfo, "logicalDensityDpi", prefs2.getInt(packageName, defaultDpi));
                }
            }

            ;
        });
        XposedHelpers.findAndHookMethod(Display.class, "getMetrics", DisplayMetrics.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = AndroidAppHelper.currentPackageName();
                if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && prefs2.contains(packageName)) {
                    Object mDisplayInfo = XposedHelpers.getObjectField(param.thisObject, "mDisplayInfo");
                    int dpi = prefs2.getInt(packageName, defaultDpi);
                    XposedHelpers.setIntField(mDisplayInfo, "logicalDensityDpi", dpi);
                    DisplayMetrics displayMetrics = (DisplayMetrics) param.args[0];
                    displayMetrics.scaledDensity = dpi / 160.0f;
                    displayMetrics.densityDpi = dpi;
                    displayMetrics.density = dpi / 160.0f;
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
                                dpi = prefs2.getInt(packageName, defaultDpi);
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
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        final String packageName = loadPackageParam.packageName;
        prefs.reload();
        prefs2.reload();
        final int defaultDpi = prefs.getInt("xposed_default_dpi", 480);

        if (packageName.equals("com.android.systemui")) {
            if (useDefaultConfig || prefs.getBoolean("xposed_hide_su", false)) {
                new SystemUI().hideSUIcon(loadPackageParam);
            }
        }

        //用于检查xposed是否激活
        else if (packageName.equals("com.omarea.vboot")) {
            new ActiveCheck().isActive(loadPackageParam);
        }

        //王者荣耀 高帧率模式
        else if (packageName.equals("com.tencent.tmgp.sgame")) {
            if (useDefaultConfig || prefs.getBoolean("xposed_hight_fps", false)) {
                new DeviceInfo().simulationX20(loadPackageParam);
            }
        }
        if (useDefaultConfig || prefs.getBoolean("xposed_webview_debug", false)) {
            new WebView().allowDebug();
        }
        if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && (prefs2.contains(packageName))) {
            XposedHelpers.findAndHookMethod("android.app.Application", loadPackageParam.classLoader, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    Context context = (Context) param.args[0];
                    context.getResources().getDisplayMetrics().density = prefs2.getInt(packageName, defaultDpi) / 160.0f;
                    context.getResources().getDisplayMetrics().densityDpi = prefs2.getInt(packageName, defaultDpi);
                    context.getResources().getDisplayMetrics().scaledDensity = prefs2.getInt(packageName, defaultDpi) / 160.0f;
                }
            });

            XposedHelpers.findAndHookMethod("android.util.DisplayMetrics", loadPackageParam.classLoader, "setTo", DisplayMetrics.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    String packageName = AndroidAppHelper.currentPackageName();
                    if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && prefs2.contains(packageName)) {
                        DisplayMetrics displayMetrics = (DisplayMetrics) (param.args[0]);
                        if (displayMetrics != null) {
                            int dpi = prefs2.getInt(packageName, defaultDpi);
                            displayMetrics.density = dpi / 160.0f;
                            displayMetrics.densityDpi = dpi;
                            displayMetrics.scaledDensity = dpi / 160.0f;
                            XposedBridge.log("setTo======" + displayMetrics.densityDpi);
                        }
                    }
                }
            });
            XposedHelpers.findAndHookMethod("android.util.DisplayMetrics", loadPackageParam.classLoader, "getRealMetrics", DisplayMetrics.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    String packageName = AndroidAppHelper.currentPackageName();
                    if ((useDefaultConfig || prefs.getBoolean("xposed_dpi_fix", false)) && prefs2.contains(packageName)) {
                        DisplayMetrics displayMetrics = (DisplayMetrics) (param.args[0]);
                        if (displayMetrics != null) {
                            int dpi = prefs2.getInt(packageName, defaultDpi);
                            displayMetrics.density = dpi / 160.0f;
                            displayMetrics.densityDpi = dpi;
                            displayMetrics.scaledDensity = dpi / 160.0f;
                            XposedBridge.log("getRealMetrics======" + displayMetrics.densityDpi);
                        }
                    }
                }
            });
        }
    }
}
