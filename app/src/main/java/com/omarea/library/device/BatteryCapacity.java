package com.omarea.library.device;

import android.annotation.SuppressLint;
import android.content.Context;

public class BatteryCapacity {
    /**
     * 获取电池容量 mAh
     * 源头文件:frameworks/base/core/res\res/xml/power_profile.xml
     * Java 反射文件：frameworks\base\core\java\com\android\internal\os\PowerProfile.java
     */
    @SuppressLint("PrivateApi")
    public double getBatteryCapacity(Context context) {
        Object mPowerProfile;
        double batteryCapacity = 0;
        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

        try {
            mPowerProfile = Class.forName(POWER_PROFILE_CLASS).getConstructor(Context.class).newInstance(context);
            batteryCapacity = (int) ((double) Class.forName(POWER_PROFILE_CLASS).getMethod("getBatteryCapacity").invoke(mPowerProfile));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return batteryCapacity;
    }
}
