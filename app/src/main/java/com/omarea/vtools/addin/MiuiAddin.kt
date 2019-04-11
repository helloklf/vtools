package com.omarea.vtools.addin

import android.content.Context
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.shared.FileWrite
import com.omarea.shared.MagiskExtend

/**
 * Created by Hello on 2018/03/22.
 */

class MiuiAddin(private var context: Context) : AddinBase(context) {
    fun hideSearch() {
        FileWrite.writePrivateFile(context.assets, "com.android.systemui", "com.android.systemui", context)
        if (MagiskExtend.moduleInstalled()) {
            MagiskExtend.replaceSystemFile("/system/media/theme/default/com.android.systemui", "${FileWrite.getPrivateFileDir(context)}/com.android.systemui")

            Toast.makeText(context, "已通过Magisk更改参数，请重启手机~", Toast.LENGTH_LONG).show()
        } else {
            command = StringBuilder()
                    .append(CommonCmds.MountSystemRW)
                    .append("cp ${FileWrite.getPrivateFileDir(context)}/com.android.systemui /system/media/theme/default/com.android.systemui\n")
                    .append("chmod 0755 /system/media/theme/default/com.android.systemui\n")
                    .toString()

            super.run()
        }
    }
}
