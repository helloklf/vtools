package com.omarea.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AppListHelper2 {
    public ArrayList<ApplicationInfo> getUninstalledApp(Context context) {
        ArrayList<ApplicationInfo> applicationInfos = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packageInfos =  pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo item: packageInfos) {
            try {
                ApplicationInfo applistionInfo = pm.getApplicationInfo(item.packageName, 0);
                if (applistionInfo == null) {
                    ApplicationInfo uninstallApp = pm.getApplicationInfo(item.packageName,  PackageManager.MATCH_UNINSTALLED_PACKAGES);
                    applicationInfos.add(uninstallApp);
                }
            } catch (Exception ex) {
                try {
                    ApplicationInfo uninstallApp = pm.getApplicationInfo(item.packageName,  PackageManager.MATCH_UNINSTALLED_PACKAGES);
                    applicationInfos.add(uninstallApp);
                } catch (Exception ex2) {
                }
            }
        }

        /*
        for (ApplicationInfo applicationInfo: applicationInfos) {
            Log.d("UNINSTALLED_PACKAGES", applicationInfo.packageName);
        }
        */
        return applicationInfos;
    }
}
