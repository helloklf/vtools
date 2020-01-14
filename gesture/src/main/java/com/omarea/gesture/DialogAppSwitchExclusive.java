package com.omarea.gesture;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import com.omarea.gesture.util.AppInfo;
import com.omarea.gesture.util.AppListHelper;
import com.omarea.gesture.util.Recents;

import java.util.ArrayList;
import java.util.Set;

public class DialogAppSwitchExclusive {
    public void openDialog(final Context context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context).setCancelable(false);

        final SharedPreferences configFile = context.getSharedPreferences(SpfConfig.AppSwitchBlackList, Context.MODE_PRIVATE);
        final Set<String> inListApps = configFile.getAll().keySet();

        final ArrayList<AppInfo> appInfos = new AppListHelper().loadAppList(context);
        final String[] appNames = new String[appInfos.size()];
        final boolean[] status = new boolean[appInfos.size()];
        for (int i = 0; i < appInfos.size(); i++) {
            status[i] = inListApps.contains(appInfos.get(i).packageName);
            appNames[i] = appInfos.get(i).appName;
        }

        alertDialog
                .setTitle(context.getString(R.string.exclude_app))
                .setMultiChoiceItems(appNames,
                        status, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                status[which] = isChecked;
                            }
                        })
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final SharedPreferences.Editor config = configFile.edit();
                        config.clear();
                        for (int i = 0; i < status.length; i++) {
                            if (status[i]) {
                                config.putBoolean(appInfos.get(i).packageName, true);
                            }
                        }
                        config.apply();
                        try {
                            Intent intent = new Intent(context.getString(R.string.app_switch_changed));
                            context.sendBroadcast(intent);
                        } catch (Exception ignored) {
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}
