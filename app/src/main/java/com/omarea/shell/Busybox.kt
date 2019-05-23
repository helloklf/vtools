package com.omarea.shell

import android.app.AlertDialog
import android.content.Context
import com.omarea.shared.FileWrite
import com.omarea.shared.MagiskExtend
import com.omarea.shell.units.BusyboxInstallerUnit
import com.omarea.vtools.R
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
    private fun useMagiskModuleInstall (context: Context) {
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
        AlertDialog.Builder(context)
                .setTitle("已完成")
                .setMessage("已通过Magisk安装了Busybox，现在需要重启手机才能生效，立即重启吗？")
                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                    KeepShellPublic.doCmdSync("sync\nsleep 2\nreboot\n")
                })
                .setNegativeButton(R.string.btn_cancel, { _, _ ->
                })
                .create()
                .show()
    }

    fun forceInstall(next: Runnable? = null) {
        val privateBusybox = FileWrite.getPrivateFilePath(context, "busybox")
        if (!(File(privateBusybox).exists() || FileWrite.writePrivateFile(context.assets, "busybox.zip", "busybox", context) == privateBusybox)) {
            return
        }
        if (!busyboxInstalled()) {
            val dialog = AlertDialog.Builder(context)
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
                                if (MagiskExtend.magiskSupported()) {
                                    useMagiskModuleInstall(context)
                                    return@setPositiveButton
                                }

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
                                    val dialog = AlertDialog.Builder(context)
                                            .setTitle("安装Busybox失败")
                                            .setMessage("已尝试自动安装Busybox，但它依然不可用。也许System分区没被解锁。因此，部分功能可能无法使用！")
                                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                            })
                                            .create()
                                    dialog.window!!.setWindowAnimations(R.style.windowAnim)
                                    dialog.show()
                                }
                                next?.run()
                            }
                    )
                    .setCancelable(false)
                    .create()
            dialog.window!!.setWindowAnimations(R.style.windowAnim)
            dialog.show()
        } else {
            BusyboxInstallerUnit().installShellTools()
            next?.run()
        }
    }

}
