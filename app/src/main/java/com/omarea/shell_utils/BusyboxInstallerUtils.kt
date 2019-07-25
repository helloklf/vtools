package com.omarea.shell_utils

import com.omarea.common.shell.KeepShellPublic

/**
 * Created by Hello on 2017/11/01.
 */

class BusyboxInstallerUtils {
    //安装Shell工具
    fun installShellTools() {
        Thread(Runnable {
            KeepShellPublic.doCmdSync("busybox --install /system/xbin\n")
        }).start()
    }
}
