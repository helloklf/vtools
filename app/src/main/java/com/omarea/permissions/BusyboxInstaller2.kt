package com.omarea.permissions

import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.vtools.R
import java.io.File

class BusyboxInstaller2(private var context: Context) {
    fun busyboxInstalled (): Boolean {
        val installPath = context.getString(R.string.toolkit_install_path)
        val absInstallPath = FileWrite.getPrivateFilePath(context, installPath)
        return File(absInstallPath + "/md5sum").exists()
    }
    fun installPrivateBusybox(): Boolean {
        if (!busyboxInstalled()) {
            val installPath = context.getString(R.string.toolkit_install_path)
            val absInstallPath = FileWrite.getPrivateFilePath(context, installPath)
            val busyboxInstallPath = installPath + "/busybox"
            val privateBusybox = FileWrite.getPrivateFilePath(context, busyboxInstallPath)
            if (!(File(privateBusybox).exists() || FileWrite.writePrivateFile(context.assets,
                            "busybox",
                            busyboxInstallPath, context) == privateBusybox)
            ) {
                return false
            }

            val absInstallerPath = FileWrite.writePrivateShellFile("addin/install_busybox.sh", installPath + "/" + "install_busybox.sh", context)
            if (absInstallerPath != null) {
                KeepShellPublic.doCmdSync("sh $absInstallerPath $absInstallPath")
            }
        }

        return true
    }
}
