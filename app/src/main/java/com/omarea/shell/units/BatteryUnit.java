package com.omarea.shell.units;

import com.omarea.shell.KernelProrp;

import java.io.File;

/**
 * Created by Hello on 2017/11/01.
 */

public class BatteryUnit {
    //是否兼容此设备
    public boolean isSupport() {
        return true;
    }

    //获取电池容量
    public String getBatteryMAH() {
        String path = "";
        if (new File("/sys/class/power_supply/battery/charge_full").exists()) {
            path = "/sys/class/power_supply/battery/charge_full";
        } else if (new File("/sys/class/power_supply/battery/charge_full_design").exists()) {
            path = "/sys/class/power_supply/battery/charge_full_design";
        } else if (new File("/sys/class/power_supply/battery/full_bat").exists()) {
            path = "/sys/class/power_supply/battery/full_bat";
        } else {
            return "? mAh";
        }
        String txt = KernelProrp.getProp(path);
        if (txt == null || txt.trim().length() == 0)
            return "? mAh";
        if (txt.length() > 4)
            return txt.substring(0, 4) + " mAh";
        return txt + " mAh";
    }

    //获取充电速度
    public String getChangeMAH() {
        // /sys/class/power_supply/battery/current_now
        String path = "";
        if (new File("/sys/class/power_supply/battery/current_now").exists()) {
            path = "/sys/class/power_supply/battery/current_now";
        } else if (new File("/sys/class/power_supply/battery/BatteryAverageCurrent").exists()) {
            path = "/sys/class/power_supply/battery/BatteryAverageCurrent";
        } else {
            return "? mA";
        }
        String txt = KernelProrp.getProp(path);

        try {
            int ma = Math.abs(Integer.parseInt(txt));
            if (ma < 1000)
                return ma + " mA";
            return (ma / 1000) + " mA";
        } catch (Exception ex) {
            return "? mA";
        }
    }
}
