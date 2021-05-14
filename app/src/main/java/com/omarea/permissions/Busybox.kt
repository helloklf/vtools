package com.omarea.permissions

import android.content.Context
import android.os.Build
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.vtools.R
import java.io.File
import java.util.*

/** 检查并安装Busybox
 * Created by helloklf on 2017/6/3.
 */

class Busybox(private var context: Context) {
    companion object {
        //是否已经安装busybox
        fun systemBusyboxInstalled(): Boolean {
            if (
                    File("/sbin/busybox").exists() ||
                    File("/system/xbin/busybox").exists() ||
                    File("/system/sbin/busybox").exists() ||
                    File("/system/bin/busybox").exists() ||
                    File("/vendor/bin/busybox").exists() ||
                    File("/vendor/xbin/busybox").exists() ||
                    File("/odm/bin/busybox").exists()) {
                return true
            }
            return try {
                Runtime.getRuntime().exec("busybox --help").destroy()
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    private fun privateBusyboxInstalled(): Boolean {
        return if (systemBusyboxInstalled()) {
            true
        } else {
            val installPath = context.getString(R.string.toolkit_install_path)
            val absInstallPath = FileWrite.getPrivateFilePath(context, installPath)
            File("$absInstallPath/md5sum").exists() && File("$absInstallPath/busybox_1_30_1").exists()
        }
    }

    private fun installPrivateBusybox(): Boolean {
        if (!(privateBusyboxInstalled() || systemBusyboxInstalled())) {
            // ro.product.cpu.abi
            val abi = Build.SUPPORTED_ABIS.joinToString(" ").toLowerCase(Locale.getDefault())
            if (!abi.contains("arm")) {
                return false
            }
            val installPath = context.getString(R.string.toolkit_install_path)
            val absInstallPath = FileWrite.getPrivateFilePath(context, installPath)

            val busyboxInstallPath = "$installPath/busybox"
            val privateBusybox = FileWrite.getPrivateFilePath(context, busyboxInstallPath)
            if (!(File(privateBusybox).exists() || FileWrite.writePrivateFile(
                            context.assets,
                            "toolkit/busybox",
                            busyboxInstallPath, context) == privateBusybox)
            ) {
                return false
            }

            val absInstallerPath = FileWrite.writePrivateShellFile(
                    "addin/install_busybox.sh",
                    "$installPath/install_busybox.sh",
                    context)
            if (absInstallerPath != null) {
                KeepShellPublic.doCmdSync("sh $absInstallerPath $absInstallPath")
            }
        }
        return true
    }

    fun forceInstall(next: Runnable) {
        val privateBusybox = FileWrite.getPrivateFilePath(context, "busybox")
        if (!(File(privateBusybox).exists() || FileWrite.writePrivateFile(context.assets, "toolkit/busybox", "busybox", context) == privateBusybox)) {
            return
        }
        if (systemBusyboxInstalled()) {
            next.run()
        } else {
            if (installPrivateBusybox()) {
                next.run()
            } else {
                DialogHelper.alert(
                        context,
                        "",
                        context.getString(R.string.busybox_nonsupport)
                ) {
                    android.os.Process.killProcess(android.os.Process.myPid())
                }.setCancelable(false)
            }
        }
    }

}
