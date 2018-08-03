package com.omarea.vtools.addin

import android.content.Context
import com.omarea.shared.CommonCmds

/**
 * Created by Hello on 2018/03/22.
 */

class BuildPropRestore(private var context: Context) : AddinBase(context) {
    fun restoreLast() {
        command = StringBuilder()
                .append(CommonCmds.MountSystemRW)
                .append("if [ -f '/system/build.bak.prop' ];then rm /system/build.prop;cp /system/build.bak.prop /system/build.prop;chmod 0644 /system/build.prop; sync;sleep 2;reboot; fi;")
                .toString()

        super.run()
    }
}
