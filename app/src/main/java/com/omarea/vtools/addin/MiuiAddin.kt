package com.omarea.vtools.addin

import android.content.Context
import com.omarea.shared.CommonCmds
import com.omarea.shared.FileWrite

/**
 * Created by Hello on 2018/03/22.
 */

class MiuiAddin(private var context: Context) : AddinBase(context) {
    fun hideSearch() {
        FileWrite.writePrivateFile(context.assets, "com.android.systemui", "com.android.systemui", context)
        command = StringBuilder()
                .append(CommonCmds.MountSystemRW)
                .append("cp ${FileWrite.getPrivateFileDir(context)}/com.android.systemui /system/media/theme/default/com.android.systemui\n")
                .append("chmod 0644 /system/media/theme/default/com.android.systemui\n")
                .toString()

        super.run()
    }
}
