package com.omarea.shell.units

import com.omarea.shell.SuDo

/**
 * Created by Hello on 2017/11/01.
 */

class BusyboxInstallerUnit {
    //安装Shell工具
    fun InstallShellTools() {
        Thread(Runnable {
            SuDo(null).execCmdSync("busybox --install /system/xbin\n")
        }).start()
    }
}
