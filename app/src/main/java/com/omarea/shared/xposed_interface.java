package com.omarea.shared;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by helloklf on 2016/10/1.
 */
public class xposed_interface implements IXposedHookLoadPackage {
    //public static ServiceHelper serviceHelper;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        final String packageName = loadPackageParam.packageName;

        //用于检查xposed是否激活
        if (packageName.equals("com.omarea.vboot")) {
            XposedHelpers.findAndHookMethod("com.omarea.shared.xposed_check", loadPackageParam.classLoader, "xposedIsRunning", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    param.setResult(true);
                }
            });
        }

        //王者荣耀 高帧率模式 - 伪装MI5
        else if (packageName.equals("com.tencent.tmgp.sgame")) {
            XposedHelpers.findAndHookMethod(
                    "android.os.SystemProperties", loadPackageParam.classLoader, "get", String.class, String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);

                            XposedHelpers.setStaticObjectField(android.os.Build.class, "PRODUCT", "gemini");
                            XposedHelpers.setStaticObjectField(android.os.Build.class, "DEVICE", "gemini");
                            XposedHelpers.setStaticObjectField(android.os.Build.class, "MODEL", "MI 5");
                            XposedHelpers.setStaticObjectField(android.os.Build.class, "BRAND", "Xiaomi");
                            XposedHelpers.setStaticObjectField(android.os.Build.class, "MANUFACTURER", "Xiaomi");
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            String prop = (String) param.args[0];
                            switch (prop) {
                                case "ro.product.model": {
                                    param.setResult("MI 5");
                                    break;
                                }
                                case "ro.product.brand": {
                                    param.setResult("Xiaomi");
                                    break;
                                }
                                case "ro.build.fingerprint": {
                                    param.setResult("Xiaomi/gemini/gemini:7.0/NRD90M/7.3.2:user/release-keys");
                                    break;
                                }
                                case "ro.product.manufacturer": {
                                    param.setResult("Xiaomi");
                                    break;
                                }
                                case "ro.product.device": {
                                    param.setResult("gemini");
                                    break;
                                }
                                case "ro.product.name": {
                                    param.setResult("gemini");
                                    break;
                                }
                                default: {
                                    break;
                                }
                            }
                        }
                    });
        } else if (packageName.equals("com.android.camera2") ||
                packageName.equals("com.android.camera") ||
                packageName.equals("com.android.updater") ||
                //packageName.equals("de.robv.android.xposed.installer") ||
                packageName.equals("com.miui.weather2") ||
                packageName.equals("com.miui.weather")) {
            XposedHelpers.findAndHookMethod("android.app.Application", loadPackageParam.classLoader, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    Context context = (Context) param.args[0];
                    float pixels = context.getResources().getDisplayMetrics().widthPixels;
                    if(pixels==1080){
                        context.getResources().getDisplayMetrics().density = 3;
                        context.getResources().getDisplayMetrics().densityDpi = 480;
                        context.getResources().getDisplayMetrics().scaledDensity = 3;
                    }
                    else if(pixels==720){
                        context.getResources().getDisplayMetrics().density = 2;
                        context.getResources().getDisplayMetrics().densityDpi = 320;
                        context.getResources().getDisplayMetrics().scaledDensity = 2;
                    }
                    else if(pixels==540||pixels==480){
                        context.getResources().getDisplayMetrics().density = (float)(1.5);
                        context.getResources().getDisplayMetrics().densityDpi = 240;
                        context.getResources().getDisplayMetrics().scaledDensity = (float)(1.5);
                    }
                }

            });
        } else if (packageName.equals("com.waterdaaan.cpufloat") || packageName.equals("eu.chainfire.perfmon")) {
            XposedHelpers.findAndHookMethod("android.app.Application", loadPackageParam.classLoader, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    Context context = (Context) param.args[0];
                    context.getResources().getDisplayMetrics().density = (float) (context.getResources().getDisplayMetrics().density / 1.7);
                    context.getResources().getDisplayMetrics().densityDpi = (int)(context.getResources().getDisplayMetrics().densityDpi / 1.7);
                    context.getResources().getDisplayMetrics().scaledDensity = (float) (context.getResources().getDisplayMetrics().scaledDensity / 1.7);
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
