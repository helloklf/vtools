package com.omarea.vboot.addin

import android.content.Context
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.vboot.R
import java.io.File

/**
 * Created by Hello on 2018/03/22.
 */

class BatteryAddin(private var context: Context) : AddinBase(context) {
    fun deleteHistory() {
        command = StringBuilder()
                .append("rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;")
                .append(Consts.Reboot)
                .toString()

        super.run()
    }

    fun disbleCharge() {
        if (File("/sys/class/power_supply/battery/battery_charging_enabled").exists() || File("/sys/class/power_supply/battery/input_suspend").exists()) {
        } else {
            Toast.makeText(context, context.getString(R.string.device_unsupport), Toast.LENGTH_SHORT).show()
            return
        }
        command = StringBuilder().append(Consts.DisableChanger).toString()
        super.run()
    }
    fun resumeCharge() {
        if (File("/sys/class/power_supply/battery/battery_charging_enabled").exists() || File("/sys/class/power_supply/battery/input_suspend").exists()) {
        } else {
            Toast.makeText(context, context.getString(R.string.device_unsupport), Toast.LENGTH_SHORT).show()
            return
        }
        command = StringBuilder().append(Consts.ResumeChanger).toString()
        super.run()
    }
}
