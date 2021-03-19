package com.omarea.xposed;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;

import androidx.recyclerview.widget.RecyclerView;

import com.omarea.library.calculator.Flags;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.content.Context.ACTIVITY_SERVICE;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class ExcludeRecent {
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                loadPackageParam.classLoader,
                "onCreate",
                Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Activity activity = (Activity) param.thisObject;
                        if (activity != null) {
                            ActivityManager service = (ActivityManager) (activity.getSystemService(ACTIVITY_SERVICE));
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
}
