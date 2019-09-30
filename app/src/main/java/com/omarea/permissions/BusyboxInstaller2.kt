package com.omarea.permissions

import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.shell_utils.PropsUtils
import com.omarea.vtools.R
import java.io.File

class BusyboxInstaller2(private var context: Context) {
    fun busyboxInstalled(): Boolean {
        val installPath = context.getString(R.string.toolkit_install_path)
        val absInstallPath = FileWrite.getPrivateFilePath(context, installPath)
        return File(absInstallPath + "/md5sum").exists() &&  File(absInstallPath + "/busybox_1_30_1").exists()
    }

    fun installPrivateBusybox(): Boolean {
        if (!busyboxInstalled()) {
            // ro.product.cpu.abi
            val abi = PropsUtils.getProp("ro.product.cpu.abi").toLowerCase()
            if (!abi.startsWith("arm")) {
                return false
            }
            val installPath = context.getString(R.string.toolkit_install_path)
            val absInstallPath = FileWrite.getPrivateFilePath(context, installPath)

            val busyboxInstallPath = installPath + "/busybox"
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
                    installPath + "/" + "install_busybox.sh",
                    context)
            if (absInstallerPath != null) {
                KeepShellPublic.doCmdSync("sh $absInstallerPath $absInstallPath")
            }
        }

        return true
    }
}
