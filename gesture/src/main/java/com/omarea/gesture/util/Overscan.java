package com.omarea.gesture.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class Overscan {
    private static int getNavigationHeight(Context context) {
        int result = 0;
        int resourceId = 0;
        int rid = context.getResources().getIdentifier("config_showNavigationBar", "bool", "android");
        if (rid != 0) {
            resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            return context.getResources().getDimensionPixelSize(resourceId);
        } else {
            return 0;
        }
    }

    public boolean canWriteSecureSettings(Context context) {
        return PackageManager.PERMISSION_GRANTED == context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
    }

    public boolean setOverscan(Context context) {
        int navigationHeight = getNavigationHeight(context);
        if (navigationHeight > 0 && canWriteSecureSettings(context)) {
            try {
                Process process = Runtime.getRuntime().exec("sh");
                OutputStream outputStream = process.getOutputStream();
                outputStream.write(("/system/bin/wm overscan 0,0,0,-" + navigationHeight).getBytes());
                outputStream.write("\nexit\nexit\n".getBytes());
                outputStream.flush();
                InputStreamReader inputStreamReader = new InputStreamReader(process.getErrorStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    Log.e(">>>>", line);
                    bufferedReader.readLine();
                }
                return process.waitFor() == 0;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public boolean resetOverscan(Context context) {
        if (canWriteSecureSettings(context)) {
            try {
                return Runtime.getRuntime().exec("wm overscan reset").waitFor() == 0;
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
