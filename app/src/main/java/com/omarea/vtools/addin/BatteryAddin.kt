package com.omarea.vtools.addin

import android.content.Context
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.vtools.R
import java.io.File

/**
 * Created by Hello on 2018/03/22.
 */

class BatteryAddin(private var context: Context) : AddinBase(context) {
    fun deleteHistory() {
        command = StringBuilder()
                .append("rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;")
                .append(CommonCmds.Reboot)
                .toString()

        super.run()
    }
}
