package com.omarea.shared

import android.os.Environment

/**
 * Created by Hello on 2017/2/22.
 */

object CommonCmds {
    const val PACKAGE_NAME = "com.omarea.vtools"
    val SDCardDir: String = Environment.getExternalStorageDirectory().absolutePath

    const val BackUpDir = "/backups/apps/";
    val AbsBackUpDir = "$SDCardDir/backups/apps/";

    const val MountSystemRW =
            "busybox mount -o rw,remount /system\n" +
                    "mount -o rw,remount /system\n" +
                    "busybox mount -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "mount -o remount,rw /dev/block/bootdevice/by-name/system /system\n"
    const val MountVendorRW =
            "busybox mount -o rw,remount /vendor\n" +
                    "mount -o rw,remount /vendor\n"

    const val POWER_CFG_PATH = "/data/powercfg.sh"
    const val POWER_CFG_BASE = "/data/powercfg-base.sh"

    const val ExecuteConfig = "sh ${POWER_CFG_BASE};\n"
    const val ToggleMode = "sh $POWER_CFG_PATH %s;\n"

    const val DisableSELinux = "setenforce 0;\n"
    const val ResumeSELinux = "setenforce 1;\n"

    const val DisableChanger = "if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]; then echo 0 > /sys/class/power_supply/battery/battery_charging_enabled; else echo 1 > /sys/class/power_supply/battery/input_suspend; fi;setprop vtools.bp 1;\n"
    const val ResumeChanger = "if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]; then echo 1 > /sys/class/power_supply/battery/battery_charging_enabled; else echo 0 > /sys/class/power_supply/battery/input_suspend; fi;setprop vtools.bp 0;\n"
    const val Reboot = "sync;sleep 2;reboot\n"
}
