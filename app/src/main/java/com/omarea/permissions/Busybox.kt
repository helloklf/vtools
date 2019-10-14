package com.omarea.permissions

import android.app.AlertDialog
import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.BusyboxInstallerUtils
import com.omarea.shell_utils.PropsUtils
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import com.omarea.vtools.R.string.btn_cancel
import java.io.File

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
                Runtime.getRuntime().exec("busybox").destroy()
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
            val abi = PropsUtils.getProp("ro.product.cpu.abi").toLowerCase()
            if (!abi.startsWith("arm")) {
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

        val config = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        config.edit().putBoolean(SpfConfig.GLOBAL_USE_PRIVATE_BUSYBOX, true).apply()
        return true
    }
    private fun uninstallPrivateBusybox() {
        val installPath = context.getString(R.string.toolkit_install_path)
        val absInstallPath = FileWrite.getPrivateFilePath(context, installPath)
        if (absInstallPath.isNotEmpty()) {
            KeepShellPublic.doCmdSync("rm -rf $absInstallPath")
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
        MagiskExtend.replaceSystemFile("/system/xbin/busybox", privateBusybox)
        val busyboxPath = MagiskExtend.getMagiskReplaceFilePath("/system/xbin/busybox")
        val busyboxDir = File(busyboxPath).parent
        val cmd = "cd \"$busyboxDir\"\n" +
                "for applet in `./busybox --list`;\n" +
                "do\n" +
                "./busybox ln -sf busybox \$applet;\n" +
                "done\n"
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
        cmd.append("cp $privateBusybox /system/xbin/busybox\n")
        cmd.append("$privateBusybox chmod 0777 /system/xbin/busybox\n")
        cmd.append("chmod 0777 /system/xbin/busybox\n")
        cmd.append("$privateBusybox chown root:root /system/xbin/busybox\n")
        cmd.append("chown root:root /system/xbin/busybox\n")
        cmd.append("/system/xbin/busybox --install /system/xbin\n")

        KeepShellPublic.doCmdSync(cmd.toString())
        if (!systemBusyboxInstalled()) {
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
        val builder = AlertDialog.Builder(context)
                .setTitle(R.string.busybox_install_mode)
                .setMessage(R.string.busybox_install_desc)
                .setNegativeButton(R.string.busybox_install_private) { _, _ ->
                    if (installPrivateBusybox()) {
                        onSuccess?.run()
                    } else {
                        DialogHelper.animDialog(
                                AlertDialog.Builder(context)
                                        .setMessage(R.string.busybox_nonsupport).setCancelable(false).setPositiveButton(R.string.btn_exit) { _, _ ->
                                            android.os.Process.killProcess(android.os.Process.myPid())
                                        })
                    }
                }
                .setNeutralButton(R.string.busybox_install_classical) { _, _ ->
                    installUseRoot(privateBusybox, onSuccess)
                }
        if (MagiskExtend.magiskSupported()) {
            builder.setPositiveButton(R.string.busybox_install_module) { _, _ ->
                useMagiskModuleInstall(context)
            }
        }
        DialogHelper.animDialog(builder)
    }

    fun forceInstall(next: Runnable? = null) {
        val privateBusybox = FileWrite.getPrivateFilePath(context, "busybox")
        if (!(File(privateBusybox).exists() || FileWrite.writePrivateFile(context.assets, "toolkit/busybox", "busybox", context) == privateBusybox)) {
            return
        }
        if (systemBusyboxInstalled()) {
            // BusyboxInstallerUtils().installShellTools()
            next?.run()
        } else {
            val config = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
            if (config.getBoolean(SpfConfig.GLOBAL_USE_PRIVATE_BUSYBOX, false) && installPrivateBusybox()) {
                next?.run()
            } else {
                installModeChooser(privateBusybox, next)
            }
        }
    }

}
