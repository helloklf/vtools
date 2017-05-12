package com.omarea.shared;

/**
 * Created by Hello on 2017/2/22.
 */

public class Consts {
    public final static String FastChanger =
            "if [ `cat /sys/class/power_supply/battery/capacity` -lt 90 ]; then " +
                    "echo 0 > /sys/class/power_supply/battery/restricted_charging;" +
                    "echo 0 > /sys/class/power_supply/battery/safety_timer_enabled;" +
                    "echo 480 > /sys/class/power_supply/bms/temp_warm;" +
                    "echo 2000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 2500000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 3000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 3500000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 4000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "fi;";

    public final static String BP = "if [ `cat /sys/class/power_supply/battery/capacity` -gt 89 ]; then " +
            "echo 0 > /sys/class/power_supply/battery/battery_charging_enabled;" +
            "fi;";
    public final static String BPReset = "echo 1 > /sys/class/power_supply/battery/battery_charging_enabled;\n";

    public final static String ClearCache = "echo 3 > /proc/sys/vm/drop_caches";

    public final static String MountSystemRW =
            "busybox mount -o rw,remount /system\n" +
                    "mount -o rw,remount /system\n" +
                    "busybox mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n";

    public final static String InstallConfig =
            "cp /sdcard/Android/data/com.omarea.vboot/init.qcom.post_boot.sh /cache/init.qcom.post_boot.sh\n" +
                    "cp /sdcard/Android/data/com.omarea.vboot/thermal-engine.conf /cache/thermal-engine-cpuNumber.conf\n" +
                    "cp /sdcard/Android/data/com.omarea.vboot/powercfg.sh /cache/powercfg.sh\n" +
                    "chmod 0777 /cache/powercfg.sh\n" +
                    "chmod 0777 /cache/init.qcom.post_boot.sh\n" +
                    "chmod 0777 /cache/thermal-engine-cpuNumber.conf\n";

    public final static String InstallPowerToggleConfigToCache =
            "cp /sdcard/Android/data/com.omarea.vboot/powercfg.sh /cache/powercfg.sh\n" +
                    "chmod 0777 /cache/powercfg.sh\n";

    public final static String BackUpConfig =
            "if [ ! -f \"/system/etc/thermal-engine-cpuNumber-original.conf\" ]; then cp /system/etc/thermal-engine-cpuNumber.conf /system/etc/thermal-engine-cpuNumber-original.conf; fi;\n" +
                    "if [ ! -f \"/system/etc/init.qcom.post_boot-original.sh\" ]; then cp /system/etc/init.qcom.post_boot.sh /system/etc/init.qcom.post_boot-original.sh; fi;\n";

    public final static String RestoreConfig =
            "cp /system/etc/thermal-engine-cpuNumber-original.conf /system/etc/thermal-engine-cpuNumber.conf;\n" +
                    "cp /system/etc/init.qcom.post_boot-original.sh /system/etc/init.qcom.post_boot.sh;\n" +
                    "rm /system/etc/thermal-engine-cpuNumber-original.conf\n" +
                    "rm /system/etc/init.qcom.post_boot-original.sh\n" +
                    "rm /cache/powercfg.sh\n";

    public final static String ExecuteConfig = "sh /cache/init.qcom.post_boot.sh\n";
    public final static String ToggleDefaultMode = "sh /cache/powercfg.sh balance\n";
    public final static String ToggleGameMode = "sh /cache/powercfg.sh performance\n";
    public final static String TogglePowersaveMode = "sh /cache/powercfg.sh powersave\n";
    public final static String ToggleFastMode = "sh /cache/powercfg.sh fast\n";

    public final static String DisableSELinux = "setenforce 0\n";

    public final static String MiuiUninstall =
            "rm -rf /data-app\n" +

            "rm -rf /system/app/Email\n" +
            "rm -rf /system/app/BasicDreams\n" +
            "rm -rf /system/app/GameCenter\n" +
            "rm -rf /system/app/FileExplorer\n" +
            "rm -rf /system/app/MiLinkService\n" +
            "rm -rf /system/app/MiuiCompass\n" +
            "rm -rf /system/app/MiuiDaemon\n" +
            "rm -rf /system/app/mab\n" +
            "rm -rf /system/app/PhotoTable\n" +
            "rm -rf /system/app/VoiceAssist\n" +
            "rm -rf /system/app/PhotoTableVoicePrintService\n" +
            "rm -rf /system/app/PrintSpooler\n" +

            "rm -rf /system/priv-app/Mipub\n" +
            "rm -rf /system/priv-app/Music\n" +
            "rm -rf /system/priv-app/MiuiVideo\n" +
            "rm -rf /system/priv-app/Browser\n" +
            "rm -rf /system/priv-app/MiVRFramework\n" +
            "rm -rf /system/priv-app/MiGameCenterSDKService\n" +
            "rm -rf /system/priv-app/CleanMaster\n"+
            "sync\nreboot\n";

    public final static String DeleteLockPwd = "rm -f /data/system/*.key;rm -f /data/system/locksettings.db*;reboot;";

    public final static String GetBatteryStatus = "/sys/class/power_supply/battery/status\n";

    public final static String GetCPUName = "getprop ro.board.platform\n";

    public final static String RebootShutdown = "reboot -p\n";
    public final static String RebootRecovery = "reboot recovery\n";
    public final static String RebootBootloader = "reboot bootloader\n";
    public final static String RebootOEM_EDL = "reboot edl\n";
    public final static String Reboot = "reboot\n";
    public final static String RebootHot = "busybox killall system_server\n";

    public final static String RMQQStyles =
            "rm -rf /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "echo \"\" > /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "echo \"\" > /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "echo \"\" > /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "pgrep com.tencent.mobileqq |xargs kill -9\n";

    public final static String ResetQQStyles =
            "rm -rf /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "pgrep com.tencent.mobileqq |xargs kill -9\n";

    public final static String RMThermal =
            "cp /system/vendor/bin/thermal-engine /system/vendor/bin/thermal-engine.bak\n" +
                    "rm -f /system/vendor/bin/thermal-engine\n" +

                    "cp /system/vendor/lib64/libthermalclient.so /system/vendor/lib64/libthermalclient.so.bak\n" +
                    "rm -f /system/vendor/lib64/libthermalclient.so\n" +

                    "cp /system/vendor/lib64/libthermalioctl.so /system/vendor/lib64/libthermalioctl.so.bak\n" +
                    "rm -f /system/vendor/lib64/libthermalioctl.so\n" +

                    "cp /system/vendor/lib/libthermalclient.so /system/vendor/lib/libthermalclient.so.bak\n" +
                    "rm -f /system/vendor/lib/libthermalclient.so\n";

    public final static String ResetThermal =
            "cp /system/vendor/bin/thermal-engine.bak /system/vendor/bin/thermal-engine\n" +
                    "rm -f /system/vendor/bin/thermal-engine.bak\n" +

                    "cp /system/vendor/lib64/libthermalclient.so.bak /system/vendor/lib64/libthermalclient.so\n" +
                    "rm -f /system/vendor/lib64/libthermalclient.so.bak\n" +

                    "cp /system/vendor/lib64/libthermalioctl.so.bak /system/vendor/lib64/libthermalioctl.so\n" +
                    "rm -f /system/vendor/lib64/libthermalioctl.so.bak\n" +

                    "cp /system/vendor/lib/libthermalclient.so.bak /system/vendor/lib/libthermalclient.so\n" +
                    "rm -f /system/vendor/lib/libthermalclient.so.bak\n";
}
