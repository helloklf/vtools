package com.omarea.shell

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.omarea.shared.FileWrite
import com.omarea.shared.MagiskExtend
import com.omarea.shell.units.BusyboxInstallerUnit
import com.omarea.ui.ProgressBarDialog
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

    fun forceInstall2(next: Runnable? = null) {
        val dialog = AlertDialog.Builder(context)
                .setTitle("需要安装Busybox")
                .setMessage("您当前系统未安装Busybox，并且Scene也没有集成安装包，需要联网下载安装包才能继续！")
                .setPositiveButton(R.string.btn_confirm) { dialog, which ->
                    val downloadUrl = "http://vtools.oss-cn-beijing.aliyuncs.com/addin/busybox.zip"// "http://47.106.224.127/publish/app-release.apk"
                    /*
                    try {
                        val intent = Intent()
                        intent.data = Uri.parse(downloadUrl)
                        context.startActivity(intent)
                    } catch (ex: java.lang.Exception) {
                        Toast.makeText(context, "启动下载失败！", Toast.LENGTH_SHORT).show()
                    }
                    */
                    //创建下载任务,downloadUrl就是下载链接
                    val request = DownloadManager.Request(Uri.parse(downloadUrl));
                    //指定下载路径和下载文件名
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "busybox.zip");
                    request.setTitle("Busybox.zip")
                    request.setDescription(downloadUrl)
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                    request.setMimeType("application/x-zip-compressed")
                    request.setVisibleInDownloadsUi(true)
                    //在通知栏显示下载进度
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    //获取下载管理器
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    //将下载任务加入下载队列，否则不会进行下载
                    val taskId = downloadManager.enqueue(request)

                    val progressBarDialog = ProgressBarDialog(context)
                    progressBarDialog.showDialog("正在下载Busybox...")

                    val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                    context.registerReceiver(object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            progressBarDialog.hideDialog()
                            val id = intent!!.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                            if (id == taskId) {
                                val path = getRealFilePath(context!!, downloadManager.getUriForDownloadedFile(taskId))
                                if (path != null) {
                                    if (MagiskExtend.magiskSupported()) {
                                        useMagiskModuleInstall(context)
                                        return
                                    }
                                    val privateBusybox = FileWrite.getPrivateFilePath(context, "busybox")
                                    val cmd = StringBuilder("cp '$path' /cache/busybox;\n")
                                    cmd.append("chmod 7777 /cache/busybox;\n")
                                    cmd.append("mkdir -p '$privateBusybox';\n")
                                    cmd.append("rm -f '$privateBusybox';\n")
                                    cmd.append("cp '$path' '$privateBusybox';\n")
                                    cmd.append("chmod 7777 '$privateBusybox';\n")
                                    cmd.append("/cache/busybox mount -o rw,remount /system\n" +
                                            "/cache/busybox mount -f -o rw,remount /system\n" +
                                            "mount -o rw,remount /system\n" +
                                            "/cache/busybox mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                                            "mount -f -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                                            "/cache/busybox mount -o rw,remount /system/xbin\n" +
                                            "/cache/busybox mount -f -o rw,remount /system/xbin\n" +
                                            "mount -o rw,remount /system/xbin\n")
                                    cmd.append("cp /cache/busybox /system/xbin/busybox;")
                                    cmd.append("/cache/busybox chmod 0777 /system/xbin/busybox;")
                                    cmd.append("chmod 0777 /system/xbin/busybox;")
                                    cmd.append("/cache/busybox chown root:root /system/xbin/busybox;")
                                    cmd.append("chown root:root /system/xbin/busybox;")
                                    cmd.append("/system/xbin/busybox --install /system/xbin;")

                                    KeepShellPublic.doCmdSync(cmd.toString())
                                    if (busyboxInstalled()) {
                                        next!!.run()
                                    } else {
                                        busyboxDownloadFail()
                                    }
                                } else {
                                    busyboxDownloadFail()
                                }
                            }
                        }
                    }, intentFilter)
                }
                .setNegativeButton(R.string.btn_cancel) { dialog, which ->
                    busyboxDownloadFail()
                }
                .setCancelable(false)
                .create()
        dialog.window!!.setWindowAnimations(R.style.windowAnim)
        dialog.show()
    }

    private fun busyboxDownloadFail() {
        val dialog = AlertDialog.Builder(context)
                .setTitle("安装失败")
                .setMessage("在线安装Busybox失败，Scene无法继续运行，请用其它方式安装！\n")
                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                    android.os.Process.killProcess(android.os.Process.myPid())
                })
                .setCancelable(false)
                .create()
        dialog.window!!.setWindowAnimations(R.style.windowAnim)
        dialog.show()
    }

    fun forceInstall(next: Runnable? = null) {
        val privateBusybox = FileWrite.getPrivateFilePath(context, "busybox")
        if (!(File(privateBusybox).exists() || FileWrite.writePrivateFile(context.assets, "busybox.zip", "busybox", context) == privateBusybox)) {
            forceInstall2(next)
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

    fun getRealFilePath(context: Context, uri: Uri?): String? {
        if (null == uri) return null
        val scheme = uri.scheme
        var data: String? = null
        if (scheme == null)
            data = uri.path
        else if (ContentResolver.SCHEME_FILE == scheme) {
            data = uri.path
        } else if (ContentResolver.SCHEME_CONTENT == scheme) {
            val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null)
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                    if (index > -1) {
                        data = cursor.getString(index)
                    }
                }
                cursor.close()
            }
        }
        return data
    }
}
