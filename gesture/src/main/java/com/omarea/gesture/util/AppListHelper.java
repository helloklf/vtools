package com.omarea.gesture.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

public class AppListHelper {
    public ArrayList<AppInfo> loadAppList(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        Intent queryActIntent = new Intent(Intent.ACTION_MAIN, null);
        queryActIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        // queryActIntent.setPackage(info.packageName);
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(queryActIntent, 0);

        ArrayList<AppInfo> packages = new ArrayList<>();
        for (final ResolveInfo resolveInfo : resolveInfoList) {
            String appName = (String) resolveInfo.loadLabel(packageManager);
            packages.add(new AppInfo() {{
                appName = (String) resolveInfo.loadLabel(packageManager);
                packageName = resolveInfo.activityInfo.packageName;
            }});
        }

        return packages;
    }
}
