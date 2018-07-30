package com.omarea.shared

import android.os.Environment

/**
 * Created by Hello on 2017/2/22.
 */

public object Consts {
    const val PACKAGE_NAME = "com.omarea.vtools"
    public val SDCardDir = Environment.getExternalStorageDirectory().absolutePath

    const val BackUpDir = "/backups/apps/";
    val AbsBackUpDir = "$SDCardDir/backups/apps/";
    const val FastChangerBase =
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

    const val MountSystemRW =
            "busybox mount -o rw,remount /system\n" +
                    "busybox mount -f -o rw,remount /system\n" +
                    "mount -o rw,remount /system\n" +
                    "busybox mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "busybox mount -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "mount -o remount,rw /dev/block/bootdevice/by-name/system /system\n"
    const val MountSystemRW2 =
            "/cache/busybox mount -o rw,remount /system\n" +
                    "/cache/busybox mount -f -o rw,remount /system\n" +
                    "mount -o rw,remount /system\n" +
                    "/cache/busybox mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "/cache/busybox mount -o rw,remount /system/xbin\n" +
                    "/cache/busybox mount -f -o rw,remount /system/xbin\n" +
                    "mount -o rw,remount /system/xbin\n"

    const val POWER_CFG_PATH = "/data/powercfg.sh"
    const val POWER_CFG_BASE = "/data/powercfg-base.sh"

    const val ExecuteConfig = "sh ${POWER_CFG_BASE};\n"
    const val ToggleMode = "sh $POWER_CFG_PATH %s;\n"

    const val DisableSELinux = "setenforce 0;\n"
    const val ResumeSELinux = "setenforce 1;\n"

    const val DeleteLockPwd = "rm -f /data/system/*.key;rm -f /data/system/locksettings.db*;reboot;"

    const val DisableChanger = "if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]; then echo 0 > /sys/class/power_supply/battery/battery_charging_enabled; else echo 1 > /sys/class/power_supply/battery/input_suspend; fi;setprop vtools.bp 1;\n"
    const val ResumeChanger = "if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]; then echo 1 > /sys/class/power_supply/battery/battery_charging_enabled; else echo 0 > /sys/class/power_supply/battery/input_suspend; fi;setprop vtools.bp 0;\n"
    const val DeleteBatteryHistory = "rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;sync;sleep 2; reboot;";
    const val Reboot = "sync;sleep 2;reboot\n"

    const val RMThermal =
            "cp /system/vendor/bin/thermal-engine /system/vendor/bin/thermal-engine.bak\n" +
                    "rm -f /system/vendor/bin/thermal-engine\n" +

                    "cp /system/vendor/lib64/libthermalclient.so /system/vendor/lib64/libthermalclient.so.bak\n" +
                    "rm -f /system/vendor/lib64/libthermalclient.so\n" +

                    "cp /system/vendor/lib64/libthermalioctl.so /system/vendor/lib64/libthermalioctl.so.bak\n" +
                    "rm -f /system/vendor/lib64/libthermalioctl.so\n" +

                    "cp /system/vendor/lib/libthermalclient.so /system/vendor/lib/libthermalclient.so.bak\n" +
                    "rm -f /system/vendor/lib/libthermalclient.so\n"

    const val ResetThermal =
            "cp /system/vendor/bin/thermal-engine.bak /system/vendor/bin/thermal-engine\n" +
                    "rm -f /system/vendor/bin/thermal-engine.bak\n" +

                    "cp /system/vendor/lib64/libthermalclient.so.bak /system/vendor/lib64/libthermalclient.so\n" +
                    "rm -f /system/vendor/lib64/libthermalclient.so.bak\n" +

                    "cp /system/vendor/lib64/libthermalioctl.so.bak /system/vendor/lib64/libthermalioctl.so\n" +
                    "rm -f /system/vendor/lib64/libthermalioctl.so.bak\n" +

                    "cp /system/vendor/lib/libthermalclient.so.bak /system/vendor/lib/libthermalclient.so\n" +
                    "rm -f /system/vendor/lib/libthermalclient.so.bak\n"

    const val isRootUser = "if [[ `id -u 2>&1` = '0' ]]; then\n" +
            "\techo 'root';\n" +
            "elif [[ `\$UID` = '0' ]]; then\n" +
            "\techo 'root';\n" +
            "elif [[ `whoami 2>&1` = 'root' ]]; then\n" +
            "\techo 'root';\n" +
            "elif [[ `set | grep 'USER_ID=0'` = 'USER_ID=0' ]]; then\n" +
            "\techo 'root';\n" +
            "else\n" +
            "\texit -1;\n" +
            "fi;"
}
