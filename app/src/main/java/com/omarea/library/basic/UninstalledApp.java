package com.omarea.library.basic;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class UninstalledApp {
    // 卸载的、隐藏的
    public ArrayList<ApplicationInfo> getUninstalledApp(Context context) {
        ArrayList<ApplicationInfo> applicationInfos = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo item : packages) {
            try {
                pm.getApplicationInfo(item.packageName, 0);
            } catch (Exception ex) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        ApplicationInfo uninstallApp = pm.getApplicationInfo(item.packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES);
                        applicationInfos.add(uninstallApp);
                    }
                } catch (Exception ignored) {
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
