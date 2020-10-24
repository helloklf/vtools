package com.omarea.xposed;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AppFreezeInjector {
    private PackageManager packageManager;

    private String getHomeLauncher(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
        String currentHomePackage = resolveInfo.activityInfo.packageName;
        return currentHomePackage;
    }

    private boolean unfreeze(Context context, String packageName) {
        // 在手机刚开机未解锁的情况下，访问SceneContentProvider 会出现Unknown URL
        try {
            Uri uri = Uri.parse("content://com.omarea.vtools.SceneFreezeProvider");
            ContentResolver contentProvider = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("packageName", packageName);
            contentValues.put("source", context.getPackageName());
            return contentProvider.insert(uri, contentValues) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private void packageProcess(Context context, String packageName) {
        try {
            if (packageManager == null) {
                packageManager = context.getApplicationContext().getPackageManager();
            }

            Method method = packageManager.getClass().getMethod("isPackageSuspended", String.class);
            if ((Boolean) (method.invoke(packageManager, packageName))) {
                // 方式3：通过Scene解冻后再启动
                if (unfreeze(context, packageName)) {
                    // -> 通过Scene解冻就完事了咯
                    Toast.makeText(context, "已通过Scene解冻应用：" + packageName, Toast.LENGTH_SHORT).show();
                }

                // 方式1： 由Scene通过ROOT启动
                // intent.setClassName("com.omarea.vtools", "com.omarea.vtools.activities.ActivityQuickStart");
                // intent.putExtra("packageName", packageName);
                // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                // 方式2：由桌面自己解冻再启动（通常没有权限）
                // // packageManager.setPackagesSuspended(packageNames, true, null, null, dialogMessage);
                // Method setPackagesSuspended = packageManager.getClass().getMethod("setPackagesSuspended", String[].class, boolean.class, PersistableBundle.class, PersistableBundle.class, String.class);
                // setPackagesSuspended.invoke(packageManager, new String[]{ packageName }, false, null, null, "通过Scene启动冻结的应用！");
            }
        } catch (Exception ex) {
            // Toast.makeText(context, "应用偏见处理异常\nAction: " + intent.getAction() + "\ngetPackage: " + intent.getPackage() + "\nComponentName: " + (component != null ? component.getClassName() : null), Toast.LENGTH_LONG).show();
        }
    }

    void appFreezeInject(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XC_MethodHook hook = new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length > 0 && param.args[0] != null) {
                        Intent intent = (Intent) param.args[0];
                        String packageName = intent.getPackage();
                        ComponentName component = intent.getComponent();
                        if (packageName == null && component != null) {
                            packageName = component.getPackageName();
                        }
                        Context context = (Context) param.thisObject;

                        if (packageName != null && !packageName.equals(context.getPackageName())) {
                            packageProcess(context, packageName);
                        } else if (packageName == null) {
                            // Toast.makeText(context, "Action: " + intent.getAction() + "\ngetPackage: " + intent.getPackage() + "\nComponentName: " + (component != null ? component.getClassName() : null), Toast.LENGTH_LONG).show();
                        }
                    }
                    // param.setResult(0x0);
                }
            };


            // Activity打开应用
            Class<?> activityClazz = XposedHelpers.findClass(Activity.class.getName(), loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(activityClazz, "startActivityForResult", Intent.class, int.class, Bundle.class, hook);

            // Context打开应用
            Class<?> contextClass = XposedHelpers.findClass(ContextWrapper.class.getName(), loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(contextClass, "startActivity", Intent.class, hook);
            XposedHelpers.findAndHookMethod(contextClass, "startActivity", Intent.class, Bundle.class, hook);

            // 快捷设置Tile点击打开应用
            Class<?> tileServiceClass = XposedHelpers.findClass(TileService.class.getName(), loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(tileServiceClass, "startActivityAndCollapse", Intent.class, hook);

            /*
            // 搞不定...PendingIntent打开应用

            final Context[] appContext = new Context[1];
            Class<?> applicationClass = XposedHelpers.findClass(Application.class.getName(), loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(applicationClass, "attachBaseContext", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    appContext[0] = (Context) param.thisObject;
                }
            });

            // PendingIntent打开应用（如 通知、快捷方式）
            Class<?> pendingIntentClass = XposedHelpers.findClass(PendingIntent.class.getName(), loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(pendingIntentClass, "sendAndReturnResult", Context.class, int.class, Intent.class,
                    PendingIntent.OnFinished.class, Handler.class,
                    String.class, Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            PendingIntent pendingIntent = (PendingIntent) param.thisObject;
                            String packageName = pendingIntent.getCreatorPackage();
                            packageProcess(appContext[0], packageName);
                            // super.beforeHookedMethod(param);
                        }
                    });
            */
        }
    }
}
