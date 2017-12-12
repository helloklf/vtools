package com.omarea.shell

import android.app.AlertDialog
import android.content.Context
import com.omarea.shared.AppShared
import com.omarea.shared.Consts
import com.omarea.shell.units.BusyboxInstallerUnit

/**
 * Created by helloklf on 2017/6/3.
 */

class Busybox(private var context: Context) {
    //是否已经安装busybox
    private fun busyboxInstalled(): Boolean {
        try {
            Runtime.getRuntime().exec("busybox").destroy()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun forceInstall(next:Runnable? = null) {
        if (!busyboxInstalled()) {
            AlertDialog.Builder(context)
                    .setTitle("安装Busybox吗？")
                    .setMessage("你的手机似乎没有安装busybox，这会导致微工具箱无法使用，是否要立即安装（需要修改System）？")
                    .setNegativeButton(
                            "取消",
                            { _, _ ->
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                    )
                    .setPositiveButton(
                            "确定",
                            { _, _ ->
                                AppShared.WriteFile(context.assets, "busybox.zip", "busybox")
                                val cmd = StringBuilder("cp ${Consts.SDCardDir}/Android/data/${Consts.PACKAGE_NAME}/busybox /cache/busybox\nchmod 0777 /cache/busybox\n")
                                cmd.append(Consts.MountSystemRW2)
                                cmd.append("cp /cache/busybox /system/xbin/busybox\n/cache/busybox chmod 0777 /system/xbin/busybox\n")
                                SuDo(context).execCmdSync(cmd.toString())
                                if (next != null)
                                    next.run()
                            }
                    )
                    .setCancelable(false)
                    .create().show()
        } else {
            BusyboxInstallerUnit().InstallShellTools()
            if (next != null) {
                next.run()
            }
        }
    }
}
