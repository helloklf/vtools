package com.omarea.shared.helper

import android.os.Build
import com.omarea.common.shell.KeepShellPublic

class LocationHelper {
    fun enableGPS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            KeepShellPublic.doCmdSync("settings put secure location_providers_allowed -gps;settings put secure location_providers_allowed +gps")
        else
            KeepShellPublic.doCmdSync("settings put secure location_providers_allowed gps,network")
    }

    fun disableGPS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeepShellPublic.doCmdSync("settings put secure location_providers_allowed -gps")
        } else {
            KeepShellPublic.doCmdSync("settings put secure location_providers_allowed network")
        }
    }

    fun disableLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeepShellPublic.doCmdSync("settings put secure location_providers_allowed -gps,-network")
        } else {
            KeepShellPublic.doCmdSync("settings delete secure location_providers_allowed")
        }
    }
}
