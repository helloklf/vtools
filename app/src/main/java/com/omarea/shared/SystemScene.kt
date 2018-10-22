package com.omarea.shared

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.omarea.shell.KeepShellAsync

/**
 * Created by SYSTEM on 2018/07/19.
 */

class SystemScene(private var context: Context) {
    private var spfAutoConfig: SharedPreferences = context.getSharedPreferences(SpfConfig.BOOSTER_SPF_CFG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal: SharedPreferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    private var keepShell = KeepShellAsync(context)

    fun onScreenOn() {
        if (spfAutoConfig.getBoolean(SpfConfig.FORCEDOZE + SpfConfig.OFF, false)) {
            keepShell.doCmd("dumpsys deviceidle unforce\ndumpsys deviceidle enable all\n")
        }
        if (spfAutoConfig.getBoolean(SpfConfig.WIFI + SpfConfig.ON, false))
            keepShell.doCmd("svc wifi enable")

        if (spfAutoConfig.getBoolean(SpfConfig.NFC + SpfConfig.ON, false))
            keepShell.doCmd("svc nfc enable")

        if (spfAutoConfig.getBoolean(SpfConfig.DATA + SpfConfig.ON, false))
            keepShell.doCmd("svc data enable")

        if (spfAutoConfig.getBoolean(SpfConfig.GPS + SpfConfig.ON, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                keepShell.doCmd("settings put secure location_providers_allowed -gps;settings put secure location_providers_allowed +gps")
            else
                keepShell.doCmd("settings put secure location_providers_allowed gps,network")
        }
        val lowPowerMode = spfAutoConfig.getBoolean(SpfConfig.FORCEDOZE + SpfConfig.OFF, false)
        if (lowPowerMode) {
            switchLowPowerModeShell(false)
        }
    }

    fun onScreenOff() {
        if (spfAutoConfig.getBoolean(SpfConfig.WIFI + SpfConfig.OFF, false))
            keepShell.doCmd("svc wifi disable")

        if (spfAutoConfig.getBoolean(SpfConfig.NFC + SpfConfig.OFF, false))
            keepShell.doCmd("svc nfc disable")

        if (spfAutoConfig.getBoolean(SpfConfig.DATA + SpfConfig.OFF, false))
            keepShell.doCmd("svc data disable")

        if (spfAutoConfig.getBoolean(SpfConfig.GPS + SpfConfig.OFF, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                keepShell.doCmd("settings put secure location_providers_allowed -gps;")
            } else {
                keepShell.doCmd("settings put secure location_providers_allowed network")
            }
        }

        val lowPowerMode = spfAutoConfig.getBoolean(SpfConfig.FORCEDOZE + SpfConfig.ON, false)
        if (lowPowerMode || spfAutoConfig.getBoolean(SpfConfig.FORCEDOZE + SpfConfig.ON, false)){
            // 强制Doze: dumpsys deviceidle force-idle
            val applist = AppConfigStore(context).getDozeAppList()
            keepShell.doCmd("dumpsys deviceidle enable\ndumpsys deviceidle enable all\n")
            keepShell.doCmd("dumpsys deviceidle force-idle\n")
            for (item in applist) {
                keepShell.doCmd("dumpsys deviceidle whitelist -$item\nam set-inactive com.tencent.tim $item\n")
            }
            keepShell.doCmd("dumpsys deviceidle step\ndumpsys deviceidle step\ndumpsys deviceidle step\ndumpsys deviceidle step\n")
        }
        if (lowPowerMode) {
            switchLowPowerModeShell(true)
        }
    }

    private var lowPowerModeShell: String? = ""
    private fun switchLowPowerModeShell (powersave: Boolean) {
        if (lowPowerModeShell == null) {
            return
        }
        if (lowPowerModeShell!!.isEmpty()) {
            lowPowerModeShell = FileWrite.writePrivateShellFile("custom/battery/power_save_set.sh", "power_save_set.sh", context)
        }
        if (lowPowerModeShell.isNullOrEmpty()) {
            return
        }
        keepShell.doCmd("sh \"$lowPowerModeShell\" " + if (powersave) "1" else "0")
    }
}
