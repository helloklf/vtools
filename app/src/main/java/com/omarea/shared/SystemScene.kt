package com.omarea.shared

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.omarea.shell.KeepShell

/**
 * Created by SYSTEM on 2018/07/19.
 */

class SystemScene(private var context: Context) {
    private var spfAutoConfig: SharedPreferences = context.getSharedPreferences(SpfConfig.BOOSTER_SPF_CFG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal: SharedPreferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    private var keepShell = KeepShell(context)

    fun onScreenOn() {
        keepShell.doCmd("dumpsys deviceidle unforce;dumpsys deviceidle enable all;")
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
    }
}
