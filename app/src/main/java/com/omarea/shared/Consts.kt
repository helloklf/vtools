package com.omarea.shared

import android.os.Environment

/**
 * Created by Hello on 2017/2/22.
 */

public object Consts {
    val PACKAGE_NAME = "com.omarea.vboot"
    val SDCardDir = Environment.getExternalStorageDirectory().absolutePath

    val BackUpDir = "/backups/apps/";
    val AbsBackUpDir = SDCardDir + "/backups/apps/";
    val FastChangerBase =
            //"chmod 0777 /sys/class/power_supply/usb/pd_active;" +
            //"chmod 0777 /sys/class/power_supply/usb/pd_allowed;" +
            //"echo 1 > /sys/class/power_supply/usb/pd_active;" +
            //"echo 1 > /sys/class/power_supply/usb/pd_allowed;" +
            "chmod 0777 /sys/class/power_supply/main/constant_charge_current_max;" +
            "chmod 0777 /sys/class/qcom-battery/restricted_current;" +
            "chmod 0777 /sys/class/qcom-battery/restricted_charging;" +
            "echo 0 > /sys/class/qcom-battery/restricted_charging;" +
            "echo 0 > /sys/class/power_supply/battery/restricted_charging;" +
            "echo 0 > /sys/class/power_supply/battery/safety_timer_enabled;" +
            "chmod 0777 /sys/class/power_supply/bms/temp_warm;" +
            "echo 480 > /sys/class/power_supply/bms/temp_warm;" +
            "chmod 0777 /sys/class/power_supply/battery/constant_charge_current_max;"
    val FastChanger =
            "echo 0 > /sys/class/power_supply/battery/restricted_charging;" +
            "echo 0 > /sys/class/power_supply/battery/safety_timer_enabled;" +
            "chmod 0777 /sys/class/power_supply/bms/temp_warm;" +
            "echo 480 > /sys/class/power_supply/bms/temp_warm;" +
            "chmod 0777 /sys/class/power_supply/battery/constant_charge_current_max;" +
            "echo 2000000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
            "echo 2500000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
            "echo 3000000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
            "echo 3500000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
            "echo 4000000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
            "echo 4500000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
            "echo 5000000 > /sys/class/power_supply/battery/constant_charge_current_max;"

    /*
    val FastChanger =
            "if [ `cat /sys/class/power_supply/battery/capacity` -lt 85 ]; then " +
                    "echo 0 > /sys/class/power_supply/battery/restricted_charging;" +
                    "echo 0 > /sys/class/power_supply/battery/safety_timer_enabled;" +
                    "chmod 0777 /sys/class/power_supply/bms/temp_warm;" +
                    "echo 480 > /sys/class/power_supply/bms/temp_warm;" +
                    "chmod 0777 /sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 2000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 2500000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 3000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 3500000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 4000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 4500000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 5000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "fi;"
    */

    val ClearCache = "echo 3 > /proc/sys/vm/drop_caches"

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

    val ExecuteConfig = "setprop vtools.powercfg null;${POWER_CFG_BASE};\n"
    val ToggleMode = "setprop vtools.powercfg default;$POWER_CFG_PATH %s;\n"
    val ToggleDefaultMode = "setprop vtools.powercfg default;$POWER_CFG_PATH balance;\n"
    val ToggleGameMode = "setprop vtools.powercfg game;$POWER_CFG_PATH performance;\n"
    val TogglePowersaveMode = "setprop vtools.powercfg powersave;$POWER_CFG_PATH powersave;\n"
    val ToggleFastMode = "setprop vtools.powercfg fast;$POWER_CFG_PATH fast;\n"

    val DisableSELinux = "setenforce 0;\n"
    val ResumeSELinux = "setenforce 0;\n"

    val DeleteLockPwd = "rm -f /data/system/*.key;rm -f /data/system/locksettings.db*;reboot;"

    val ForceDoze = "dumpsys deviceidle force-idle\n"

    public val DisableChanger = "if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]; then echo 0 > /sys/class/power_supply/battery/battery_charging_enabled; else echo 1 > /sys/class/power_supply/battery/input_suspend; fi;setprop vtools.bp 1;\n"
    val ResumeChanger = "if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]; then echo 1 > /sys/class/power_supply/battery/battery_charging_enabled; else echo 0 > /sys/class/power_supply/battery/input_suspend; fi;setprop vtools.bp 0;\n"
    val DeleteBatteryHistory = "rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;sync;sleep 2; reboot;";
    val RebootShutdown = "reboot -p\n"
    val RebootRecovery = "reboot recovery\n"
    val RebootBootloader = "reboot bootloader\n"
    val RebootOEM_EDL = "reboot edl\n"
    val Reboot = "sync;sleep 2;reboot\n"
    val RebootHot = "busybox killall system_server\n"

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
