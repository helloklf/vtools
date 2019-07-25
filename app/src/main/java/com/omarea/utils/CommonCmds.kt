package com.omarea.utils

import android.os.Environment

/**
 * Created by Hello on 2017/2/22.
 */

object CommonCmds {
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

    const val POWER_CFG_BASE = "/data/powercfg-base.sh"

    const val ExecuteConfig = "sh ${POWER_CFG_BASE};\n"

    const val DisableSELinux = "setenforce 0;\n"
    const val ResumeSELinux = "setenforce 1;\n"

    const val Reboot = "sync;sleep 2;reboot\n"
}
