package com.omarea.shared

import java.util.*

/**
 * Created by Hello on 2017/2/22.
 */

object Consts {
    val FastChanger =
            "if [ `cat /sys/class/power_supply/battery/capacity` -lt 85 ]; then " +
                    "echo 0 > /sys/class/power_supply/battery/restricted_charging;" +
                    "echo 0 > /sys/class/power_supply/battery/safety_timer_enabled;" +
                    "echo 500 > /sys/class/power_supply/bms/temp_warm;" +
                    "echo 2000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 2500000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 3000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 3500000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "echo 4000000 >/sys/class/power_supply/battery/constant_charge_current_max;" +
                    "fi;"

    val BP = "if [ `cat /sys/class/power_supply/battery/capacity` -gt 84 ]; then " +
            "echo 0 > /sys/class/power_supply/battery/battery_charging_enabled;" +
            "fi;"
    val BPReset = "echo 1 > /sys/class/power_supply/battery/battery_charging_enabled;\n"

    val ClearCache = "echo 3 > /proc/sys/vm/drop_caches"

    val SupportCPU: ArrayList<String> = object : ArrayList<String>() {
        init {
            add("msm8992")
            add("msm8996")
        }
    }

    val MountSystemRW =
            "busybox mount -o rw,remount /system\n" +
                    "mount -o rw,remount /system\n" +
                    "busybox mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                    "mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n"

    val InstallConfig =
            "cp /sdcard/Android/data/com.omarea.vboot/init.qcom.post_boot.sh /cache/init.qcom.post_boot.sh\n" +
                    "cp /sdcard/Android/data/com.omarea.vboot/thermal-engine.conf /cache/thermal-engine-cpuNumber.conf\n" +
                    "cp /sdcard/Android/data/com.omarea.vboot/powercfg.sh /cache/powercfg.sh\n" +
                    "chmod 0777 /cache/powercfg.sh\n" +
                    "chmod 0777 /cache/init.qcom.post_boot.sh\n" +
                    "chmod 0777 /cache/thermal-engine-cpuNumber.conf\n"

    val InstallPowerToggleConfigToCache = "cp /sdcard/Android/data/com.omarea.vboot/powercfg.sh /cache/powercfg.sh\n" + "chmod 0777 /cache/powercfg.sh\n"

    val BackUpConfig = "if [ ! -f \"/system/etc/thermal-engine-cpuNumber-original.conf\" ]; then cp /system/etc/thermal-engine-cpuNumber.conf /system/etc/thermal-engine-cpuNumber-original.conf; fi;\n" + "if [ ! -f \"/system/etc/init.qcom.post_boot-original.sh\" ]; then cp /system/etc/init.qcom.post_boot.sh /system/etc/init.qcom.post_boot-original.sh; fi;\n"

    val RestoreConfig =
            "cp /system/etc/thermal-engine-cpuNumber-original.conf /system/etc/thermal-engine-cpuNumber.conf;\n" +
                    "cp /system/etc/init.qcom.post_boot-original.sh /system/etc/init.qcom.post_boot.sh;\n" +
                    "rm /system/etc/thermal-engine-cpuNumber-original.conf\n" +
                    "rm /system/etc/init.qcom.post_boot-original.sh\n" +
                    "rm /cache/powercfg.sh\n"

    val ExecuteConfig = "sh /cache/init.qcom.post_boot.sh\n"
    val ToggleDefaultMode = "sh /cache/powercfg.sh balance\n"
    val ToggleGameMode = "sh /cache/powercfg.sh performance\n"
    val TogglePowersaveMode = "sh /cache/powercfg.sh powersave\n"
    val ToggleFastMode = "sh /cache/powercfg.sh fast\n"

    val DisableSELinux = "setenforce 0\n"

    val NubiaUninstall =
            "pm disable com.android.cellbroadcastreceiver\n" +
                    "pm disable com.android.printspooler\n" +
                    "pm disable com.android.galaxy4\n" +
                    "pm disable com.android.noisefield\n" +
                    "pm disable com.android.phasebeam\n" +
                    "pm disable com.android.wallpaper.holospiral\n" +
                    "pm disable com.android.dreams.basic\n" +
                    "pm disable com.google.android.configupdater\n" +
                    "pm disable com.google.android.syncadapters.contacts\n" +
                    "pm disable com.google.android.syncadapters.calendar\n" +
                    "pm disable com.google.android.feedback\n" +
                    "pm disable com.google.android.backuptransport\n" +
                    "pm disable com.android.dreams.phototable\n" +
                    "pm disable com.google.android.partnersetup\n" +
                    "pm disable com.google.android.gsf\n" +
                    "pm disable com.google.android.gsf.login\n" +
                    "pm disable com.google.android.gms\n" +
                    "pm disable com.chaozh.iReaderNubia\n" +
                    "pm disable cn.nubia.presetpackageinstaller\n" +
                    "pm disable cn.nubia.factory\n" +
                    "pm disable cn.nubia.neostore\n" +
                    "pm disable cn.nubia.bootanimationinfo\n" +
                    "pm disable cn.nubia.ultrapower.launcher\n" +
                    "pm disable cn.nubia.email\n" +
                    "pm disable cn.nubia.exchange\n" +
                    "pm disable cn.nubia.video\n" +
                    "pm disable cn.nubia.nbgame\n" +
                    "pm disable cn.nubia.zbiglauncher.preset\n" +
                    "pm disable cn.nubia.phonemanualintegrate.preset\n" +
                    "pm disable cn.nubia.music.preset\n" +
                    "pm disable cn.nubia.nubiashop\n" +
                    "pm disable com.yulore.framework\n" +
                    "pm disable cn.nubia.yulorepage\n" +
                    "pm disable cn.nubia.festivalwallpaper\n" +
                    "pm disable cn.nubia.gallerylockscreen\n" +
                    "pm disable cn.nubia.wps_moffice\n" +
                    "pm disable cn.nubia.aftersale\n" +
                    "pm disable cn.nubia.aftersale\n" +
                    "pm disable com.sohu.inputmethod.sogou.nubia\n" +
                    "pm disable com.dolby\n" +
                    "pm disable com.dolby.daxappUI\n"

    val MiuiUninstall =
            "pm disable com.miui.yellowpage\n" +
                    "pm disable com.miui.klo.bugreport\n" +
                    //"pm disable com.xiaomi.scanner\n"+
                    //"pm disable com.android.cellbroadcastreceiver\n"+
                    //"pm disable com.android.musicfx\n"+
                    "pm disable com.miui.bugreport\n" +
                    "pm disable com.miui.systemAdSolution\n" +

                    "rm -rf /data-app\n" +

                    "rm -rf /system/app/Email\n" +
                    "rm -rf /system/app/BasicDreams\n" +
                    "rm -rf /system/app/GameCenter\n" +
                    //"rm -rf /system/app/FileExplorer\n" +
                    "rm -rf /system/app/MiLinkService\n" +
                    "rm -rf /system/app/MiuiCompass\n" +
                    "rm -rf /system/app/MiuiDaemon\n" +
                    "rm -rf /system/app/mab\n" +
                    "rm -rf /system/app/PhotoTable\n" +
                    "rm -rf /system/app/VoiceAssist\n" +
                    "rm -rf /system/app/PhotoTableVoicePrintService\n" +
                    //"rm -rf /system/app/PrintSpooler\n" +

                    "rm -rf /system/priv-app/Mipub\n" +
                    "rm -rf /system/priv-app/Music\n" +
                    "rm -rf /system/priv-app/MiuiVideo\n" +
                    //"rm -rf /system/priv-app/Browser\n" +
                    "rm -rf /system/priv-app/MiVRFramework\n" +
                    "rm -rf /system/priv-app/MiGameCenterSDKService\n" +
                    //"rm -rf /system/priv-app/CleanMaster\n" +

                    "sync\nreboot\n"

    val DeleteLockPwd = "rm -f /data/system/*.key;rm -f /data/system/locksettings.db*;reboot;"

    val ForceDoze = "dumpsys deviceidle force-idle\n"

    val GetBatteryStatus = "/sys/class/power_supply/battery/status\n"

    val GetCPUName = "getprop ro.board.platform\n"

    val DisableChanger = "echo 0 > /sys/class/power_supply/battery/battery_charging_enabled;\n"
    val ResumeChanger = "echo 1 > /sys/class/power_supply/battery/battery_charging_enabled;\n"
    val RebootShutdown = "reboot -p\n"
    val RebootRecovery = "reboot recovery\n"
    val RebootBootloader = "reboot bootloader\n"
    val RebootOEM_EDL = "reboot edl\n"
    val Reboot = "reboot\n"
    val RebootHot = "busybox killall system_server\n"

    val RMQQStyles =
            "rm -rf /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "echo \"\" > /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "echo \"\" > /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "echo \"\" > /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "pgrep com.tencent.mobileqq |xargs kill -9\n"

    val ResetQQStyles =
            "rm -rf /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "pgrep com.tencent.mobileqq |xargs kill -9\n"

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
