package com.omarea.xposed;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FullScreeProcess {
    public void addMarginBottom() {
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    //Application application = (Application)param.thisObject;
                    Context context = (Context) param.args[0];
                    ApplicationInfo applicationInfo = null;
                    try {
                        applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (applicationInfo != null) {
                        if (applicationInfo.metaData == null)
                            applicationInfo.metaData = new Bundle();
                        applicationInfo.metaData.putFloat("android.max_aspect", 2.4f);
                    }
                } catch (Exception ex) {
                    XposedBridge.log("VAddin Error：" + this.getClass().getName() + "\n" + ex.getMessage());
                }
                super.beforeHookedMethod(param);
            }
        });
    }
}
