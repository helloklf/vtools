package com.omarea.vboot;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;

import java.util.List;

public class BatteryService extends Service {
    public BatteryService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    reciver_batterychanged batteryChangedReciver;

    @Override
    public void onCreate() {
        if (batteryChangedReciver == null) {
            //监听电池改变
            batteryChangedReciver = new reciver_batterychanged();
            //启动完成
            IntentFilter ACTION_BOOT_COMPLETED = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
            registerReceiver(batteryChangedReciver, ACTION_BOOT_COMPLETED);
            //电源连接
            IntentFilter ACTION_POWER_CONNECTED = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
            registerReceiver(batteryChangedReciver, ACTION_POWER_CONNECTED);
            //电源断开
            IntentFilter ACTION_POWER_DISCONNECTED = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
            registerReceiver(batteryChangedReciver, ACTION_POWER_DISCONNECTED);
            //电量变化
            IntentFilter ACTION_BATTERY_CHANGED = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(batteryChangedReciver, ACTION_BATTERY_CHANGED);
            //电量不足
            IntentFilter ACTION_BATTERY_LOW = new IntentFilter(Intent.ACTION_BATTERY_LOW);
            registerReceiver(batteryChangedReciver, ACTION_BATTERY_LOW);
        }
    }

    @Override
    public void onDestroy() {
        if (batteryChangedReciver != null) {
            unregisterReceiver(batteryChangedReciver);
            batteryChangedReciver = null;
        }
    }

    //服务是否在运行
    public static Boolean serviceIsRunning(Context context) {
        ActivityManager m = (ActivityManager) (context.getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningServiceInfo> serviceInfos = m.getRunningServices(5000);
        for (ActivityManager.RunningServiceInfo serviceInfo : serviceInfos) {
            if (serviceInfo.service.getPackageName().equals("com.omarea.vboot")) {
                if (serviceInfo.service.getClassName().equals("com.omarea.vboot.BatteryService")) {
                    return true;
                }
            }
        }
        return false;
    }
}
