package com.omarea.shell.units

import com.omarea.shell.KeepShellSync

/**
 * Created by Hello on 2017/11/01.
 */

class BusyboxInstallerUnit {
    //安装Shell工具
    fun InstallShellTools() {
        Thread(Runnable {
            KeepShellSync.doCmdSync("busybox --install /system/xbin\n")
        }).start()
    }
}
