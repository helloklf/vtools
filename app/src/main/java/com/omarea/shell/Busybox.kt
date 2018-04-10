package com.omarea.shell

import android.app.AlertDialog
import android.content.Context
import com.omarea.shared.FileWrite
import com.omarea.shared.Consts
import com.omarea.shell.units.BusyboxInstallerUnit
import com.omarea.vboot.R

/** 检查并安装Busybox
 * Created by helloklf on 2017/6/3.
 */

class Busybox(private var context: Context) {
    //是否已经安装busybox
    private fun busyboxInstalled(): Boolean {
        return try {
            Runtime.getRuntime().exec("busybox").destroy()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun forceInstall(next: Runnable? = null) {
        if (!busyboxInstalled()) {
            AlertDialog.Builder(context)
                    .setTitle(R.string.question_install_busybox)
                    .setMessage(R.string.question_install_busybox_desc)
                    .setNegativeButton(
                            R.string.btn_cancel,
                            { _, _ ->
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                    )
                    .setPositiveButton(
                            R.string.btn_confirm,
                            { _, _ ->
                                FileWrite.WritePrivateFile(context.assets, "busybox.zip", "busybox", context)
                                val path = "${FileWrite.getPrivateFileDir(context)}busybox"
                                val cmd = StringBuilder("cp $path /cache/busybox;\n")
                                cmd.append("chmod 7777 $path;\n")
                                cmd.append("$path chmod 7777 /cache/busybox;\n")
                                cmd.append("chmod 7777 /cache/busybox;\n")
                                cmd.append(Consts.MountSystemRW2)
                                cmd.append("cp $path /system/xbin/busybox;")
                                cmd.append("$path chmod 0777 /system/xbin/busybox;")
                                cmd.append("chmod 0777 /system/xbin/busybox;")
                                cmd.append("$path chown root:root /system/xbin/busybox;")
                                cmd.append("chown root:root /system/xbin/busybox;")
                                cmd.append("/system/xbin/busybox --install /system/xbin;")

                                SuDo(context).execCmdSync(cmd.toString())
                                next?.run()
                            }
                    )
                    .setCancelable(false)
                    .create().show()
        } else {
            BusyboxInstallerUnit().InstallShellTools()
            next?.run()
        }
    }
}
