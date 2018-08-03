package com.omarea.vtools.addin

import android.content.Context

/**
 * Created by Hello on 2018/03/22.
 */

class SystemAddin(private var context: Context) : AddinBase(context) {
    fun deleteLockPwd() {
        command = StringBuilder().append("rm -f /data/system/*.key;rm -f /data/system/locksettings.db*;reboot;").toString()
        super.run()
    }

    fun dropCache() {
        command = StringBuilder().append("echo 3 > /proc/sys/vm/drop_caches").toString()
        super.run()
    }
}
