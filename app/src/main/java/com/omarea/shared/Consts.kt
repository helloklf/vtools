package com.omarea.shared

import android.os.Environment

/**
 * Created by Hello on 2017/2/22.
 */

public object Consts {
    val PACKAGE_NAME = "com.omarea.vboot"
    public val SDCardDir = Environment.getExternalStorageDirectory().absolutePath

    val BackUpDir = "/backups/apps/";
    val AbsBackUpDir = SDCardDir + "/backups/apps/";
    val FastChangerBase =
            //"chmod 0777 /sys/class/power_supply/usb/pd_active;" +
            "chmod 0777 /sys/class/power_supply/usb/pd_allowed;" +
            //"echo 1 > /sys/class/power_supply/usb/pd_active;" +
            "echo 1 > /sys/class/power_supply/usb/pd_allowed;" +
            "chmod 0777 /sys/class/power_supply/main/constant_charge_current_max;" +
            "chmod 0777 /sys/class/qcom-battery/restricted_current;" +
            "chmod 0777 /sys/class/qcom-battery/restricted_charging;" +
            "echo 0 > /sys/class/qcom-battery/restricted_charging;" +
            "echo 0 > /sys/class/power_supply/battery/restricted_charging;" +
            "echo 0 > /sys/class/power_supply/battery/safety_timer_enabled;" +
            "chmod 0777 /sys/class/power_supply/bms/temp_warm;" +
            "echo 480 > /sys/class/power_supply/bms/temp_warm;" +
            "chmod 0777 /sys/class/power_supply/battery/constant_charge_current_max;"

    val MountSystemRW =
            "busybox mount -o rw,remount /system\n" +
                    "busybox mount -f -o rw,remount /system\n" +
                    "mount -o rw,remount /system\n" +
                    "busybox mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n"
    val MountSystemRW2 =
            "/cache/busybox mount -o rw,remount /system\n" +
                    "/cache/busybox mount -f -o rw,remount /system\n" +
                    "mount -o rw,remount /system\n" +
                    "/cache/busybox mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "/cache/busybox mount -o rw,remount /system/xbin\n" +
                    "/cache/busybox mount -f -o rw,remount /system/xbin\n" +
                    "mount -o rw,remount /system/xbin\n"

    val POWER_CFG_PATH = "/data/powercfg"
    val POWER_CFG_BASE = "/data/init.qcom.post_boot.sh"

    val PowerModeState = "vtools.powercfg"
    val PowerModeApp = "vtools.powercfg_app"
    val ExecuteConfig = "setprop $PowerModeState '';setprop $PowerModeApp '';sh ${POWER_CFG_BASE};\n"
    val ToggleMode = "sh $POWER_CFG_PATH %s;\n"
    val SaveModeState = "setprop $PowerModeState %s;\n"
    val SaveModeApp = "setprop $PowerModeApp %s;\n"

    val DisableSELinux = "setenforce 0;\n"
    val ResumeSELinux = "setenforce 0;\n"

    val DeleteLockPwd = "rm -f /data/system/*.key;rm -f /data/system/locksettings.db*;reboot;"

    val DisableChanger = "if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]; then echo 0 > /sys/class/power_supply/battery/battery_charging_enabled; else echo 1 > /sys/class/power_supply/battery/input_suspend; fi;setprop vtools.bp 1;\n"
    val ResumeChanger = "if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]; then echo 1 > /sys/class/power_supply/battery/battery_charging_enabled; else echo 0 > /sys/class/power_supply/battery/input_suspend; fi;setprop vtools.bp 0;\n"
    val DeleteBatteryHistory = "rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;sync;sleep 2; reboot;";
    val Reboot = "sync;sleep 2;reboot\n"

    val RMThermal =
            "cp /system/vendor/bin/thermal-engine /system/vendor/bin/thermal-engine.bak\n" +
                    "rm -f /system/vendor/bin/thermal-engine\n" +

                    "cp /system/vendor/lib64/libthermalclient.so /system/vendor/lib64/libthermalclient.so.bak\n" +
                    "rm -f /system/vendor/lib64/libthermalclient.so\n" +

                    "cp /system/vendor/lib64/libthermalioctl.so /system/vendor/lib64/libthermalioctl.so.bak\n" +
                    "rm -f /system/vendor/lib64/libthermalioctl.so\n" +

                    "cp /system/vendor/lib/libthermalclient.so /system/vendor/lib/libthermalclient.so.bak\n" +
                    "rm -f /system/vendor/lib/libthermalclient.so\n"

    val ResetThermal =
            "cp /system/vendor/bin/thermal-engine.bak /system/vendor/bin/thermal-engine\n" +
                    "rm -f /system/vendor/bin/thermal-engine.bak\n" +

                    "cp /system/vendor/lib64/libthermalclient.so.bak /system/vendor/lib64/libthermalclient.so\n" +
                    "rm -f /system/vendor/lib64/libthermalclient.so.bak\n" +

                    "cp /system/vendor/lib64/libthermalioctl.so.bak /system/vendor/lib64/libthermalioctl.so\n" +
                    "rm -f /system/vendor/lib64/libthermalioctl.so.bak\n" +

                    "cp /system/vendor/lib/libthermalclient.so.bak /system/vendor/lib/libthermalclient.so\n" +
                    "rm -f /system/vendor/lib/libthermalclient.so.bak\n"
}
