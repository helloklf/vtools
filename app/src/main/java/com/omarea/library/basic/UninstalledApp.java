package com.omarea.library.basic;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class UninstalledApp {
    @RequiresApi(api = Build.VERSION_CODES.N)
    public ArrayList<ApplicationInfo> getUninstalledApp(Context context) {
        ArrayList<ApplicationInfo> applicationInfos = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo item : packageInfos) {
            try {
                pm.getApplicationInfo(item.packageName, 0);
            } catch (Exception ex) {
                try {
                    ApplicationInfo uninstallApp = pm.getApplicationInfo(item.packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES);
                    applicationInfos.add(uninstallApp);
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
