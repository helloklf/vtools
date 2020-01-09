package com.omarea.xposed;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MiuiHome {
    public void appFreezeInject(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XposedBridge.log("Scene Hook MIUI Home");
            XposedHelpers.findAndHookMethod(
                    XposedHelpers.findClass(
                            Activity.class.getName(),
                            loadPackageParam.classLoader
                    ),
                    "startActivity",
                    Intent.class,
                    Bundle.class,
                    new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("Scene Hook MIUI Home >> 1");
                            if (param.args.length > 0 && param.args[0] != null) {
                                XposedBridge.log("Scene Hook MIUI Home >> 2");
                                Intent intent = (Intent) param.args[0];
                                String packageName = intent.getPackage();
                                ComponentName component = intent.getComponent();
                                if (packageName == null && component != null) {
                                    packageName = component.getPackageName();
                                }

                                if (packageName != null) {
                                    XposedBridge.log("Scene Hook MIUI Home >> 3");
                                    Context context = (Context) param.thisObject;
                                    PackageManager packageManager = context.getPackageManager();
                                    Method method = packageManager.getClass().getMethod("isPackageSuspended", String.class);
                                    if ((Boolean) (method.invoke(packageManager, packageName))) {
                                        // Toast.makeText(context, "通过Scene启动冻结的应用！", Toast.LENGTH_SHORT).show();
                                        XposedBridge.log("Scene Hook MIUI Home >> 4");
                                        // 方式1： 由Scene通过ROOT启动
                                        intent.setClassName("com.omarea.vtools", "com.omarea.vtools.activities.ActivityQuickStart");
                                        intent.putExtra("packageName", packageName);

                                        // 方式2：由桌面自己解冻再启动（通常没有权限）
                                        // // packageManager.setPackagesSuspended(packageNames, true, null, null, dialogMessage);
                                        // Method setPackagesSuspended = packageManager.getClass().getMethod("setPackagesSuspended", String[].class, boolean.class, PersistableBundle.class, PersistableBundle.class, String.class);
                                        // setPackagesSuspended.invoke(packageManager, new String[]{ packageName }, false, null, null, "通过Scene启动冻结的应用！");
                                    }
                                } else {
                                    XposedBridge.log("Action: " + intent.getAction() + "getPackage: " + intent.getPackage() + "ComponentName: " + (component != null ? component.getClassName() : null));
                                }
                            }
                            // param.setResult(0x0);
                        }
                    });
        }
    }
}
