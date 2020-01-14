package com.omarea.data_collection;

import android.os.BatteryManager;

public class GlobalStatus {
    public static float batteryTemperature = -1;
    public static int batteryCapacity = -1;
    public static long batteryCurrentNow = -1;
    public static int batteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;
    public static String lastPackageName = "";
}
