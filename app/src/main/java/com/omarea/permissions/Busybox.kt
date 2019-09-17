package com.omarea.permissions

import android.app.AlertDialog
import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.BusyboxInstallerUtils
import com.omarea.vtools.R
import com.omarea.vtools.R.string.btn_cancel
import java.io.File

/** 检查并安装Busybox
 * Created by helloklf on 2017/6/3.
 */

class Busybox(private var context: Context) {
    //是否已经安装busybox
    private fun busyboxInstalled(): Boolean {
        return try {
            Runtime.getRuntime().exec("busybox").destroy()
            true
        } catch (ex: Exception) {
            false
        }
    }

    /**
     * 使用magisk模块安装busybox
     */
    private fun useMagiskModuleInstall(context: Context) {
        if (!MagiskExtend.moduleInstalled()) {
            MagiskExtend.magiskModuleInstall(context)
        }

        val privateBusybox = FileWrite.getPrivateFilePath(context, "busybox")
        MagiskExtend.replaceSystemFile("/system/xbin/busybox", privateBusybox);
        val busyboxPath = MagiskExtend.getMagiskReplaceFilePath("/system/xbin/busybox");
        val busyboxDir = File(busyboxPath).parent
        val cmd = "cd \"$busyboxDir\"\n" +
                "for applet in `./busybox --list`;\n" +
                "do\n" +
                "./busybox ln -sf busybox \$applet;\n" +
                "done\n";
        KeepShellPublic.doCmdSync(cmd)
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setMessage(R.string.busybox_installed_magisk)
                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    KeepShellPublic.doCmdSync("sync\nsleep 2\nreboot\n")
                }
                .setNegativeButton(btn_cancel) { _, _ ->
                })
    }

    private fun installUseRoot(privateBusybox: String, onSuccess: Runnable?) {
        val cmd = StringBuilder("cp $privateBusybox /cache/busybox;\n")
        cmd.append("chmod 7777 $privateBusybox;\n")
        cmd.append("$privateBusybox chmod 7777 /cache/busybox;\n")
        cmd.append("chmod 7777 /cache/busybox;\n")
        cmd.append("/cache/busybox mount -o rw,remount /system\n" +
                "/cache/busybox mount -f -o rw,remount /system\n" +
                "mount -o rw,remount /system\n" +
                "/cache/busybox mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                "mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                "/cache/busybox mount -o rw,remount /system/xbin\n" +
                "/cache/busybox mount -f -o rw,remount /system/xbin\n" +
                "mount -o rw,remount /system/xbin\n")
        cmd.append("cp $privateBusybox /system/xbin/busybox;")
        cmd.append("$privateBusybox chmod 0777 /system/xbin/busybox;")
        cmd.append("chmod 0777 /system/xbin/busybox;")
        cmd.append("$privateBusybox chown root:root /system/xbin/busybox;")
        cmd.append("chown root:root /system/xbin/busybox;")
        cmd.append("/system/xbin/busybox --install /system/xbin;")

        KeepShellPublic.doCmdSync(cmd.toString())
        if (!busyboxInstalled()) {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setMessage(R.string.busybox_install_fail)
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        onSuccess?.run()
                    })
        } else {
            onSuccess?.run()
        }
    }

    // 选择busybox安装方式
    private fun installModeChooser(privateBusybox: String, onSuccess: Runnable?) {
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle(R.string.busybox_install_mode)
                .setMessage(R.string.busybox_install_desc)
                .setNegativeButton(btn_cancel) { _, _ ->
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
                .setNeutralButton(R.string.busybox_install_classical) { _, _ ->
                    installUseRoot(privateBusybox, onSuccess)
                }
                .setPositiveButton(R.string.busybox_install_module) { _, _ ->
                    useMagiskModuleInstall(context)
                })
    }

    fun forceInstall(next: Runnable? = null) {
        val privateBusybox = FileWrite.getPrivateFilePath(context, "busybox")
        if (!(File(privateBusybox).exists() || FileWrite.writePrivateFile(context.assets, "toolkit/busybox", "busybox", context) == privateBusybox)) {
            return
        }
        if (!busyboxInstalled()) {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle(R.string.question_install_busybox)
                    .setMessage(R.string.question_install_busybox_desc)
                    .setNegativeButton(
                            btn_cancel
                    ) { _, _ ->
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                    .setPositiveButton(
                            R.string.btn_confirm
                    ) { _, _ ->
                        if (MagiskExtend.magiskSupported()) {
                            installModeChooser(privateBusybox, next)
                        } else {
                            installUseRoot(privateBusybox, next)
                        }
                    }
                    .setCancelable(false))
        } else {
            BusyboxInstallerUtils().installShellTools()
            next?.run()
        }
    }

}
