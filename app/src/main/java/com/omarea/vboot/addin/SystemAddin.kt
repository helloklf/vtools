package com.omarea.vboot.addin

import android.content.Context
import com.omarea.shared.Consts

/**
 * Created by Hello on 2018/03/22.
 */

class SystemAddin(private var context: Context) : AddinBase(context) {
    fun deleteLockPwd(){
        command = StringBuilder().append(Consts.DeleteLockPwd).toString()
        super.run()
    }

    fun dropCache() {
        command = StringBuilder().append("echo 3 > /proc/sys/vm/drop_caches").toString()
        super.run()
    }
}
