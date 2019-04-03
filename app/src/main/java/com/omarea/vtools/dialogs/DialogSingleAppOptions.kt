package com.omarea.vtools.dialogs

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.shared.MagiskExtend
import com.omarea.shared.model.Appinfo
import com.omarea.shell.CheckRootStatus
import com.omarea.vtools.R
import java.io.File

/**
 * Created by Hello on 2018/01/26.
 */

class DialogSingleAppOptions(context: Context, var app: Appinfo, handler: Handler) : DialogAppOptions(context, arrayListOf<Appinfo>(app), handler) {

    fun showSingleAppOptions() {
        when (app.appType) {
            Appinfo.AppType.USER -> showUserAppOptions()
            Appinfo.AppType.SYSTEM -> showSystemAppOptions()
            Appinfo.AppType.BACKUPFILE -> showBackupAppOptions()
            else -> {
                Toast.makeText(context, "UNSupport！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 显示用户应用选项
     */
    private fun showUserAppOptions() {
        AlertDialog.Builder(context).setTitle(app.appName)
                .setCancelable(true)
                .setItems(
                        arrayOf("打开（如果可以）",
                                "备份（apk、data）",
                                "备份（apk）",
                                "卸载",
                                "卸载（保留数据）",
                                "清空数据",
                                "清除缓存",
                                (if (app.enabled) "冻结" else "解冻"),
                                "应用详情",
                                "复制PackageName",
                                "在应用商店查看",
                                "禁用 + 隐藏",
                                "转为系统应用"), { _, which ->
                    when (which) {
                        0 -> startApp()
                        1 -> backupAll(true, true)
                        2 -> backupAll(true, false)
                        3 -> uninstallAll()
                        4 -> uninstallKeepDataAll()
                        5 -> clearAll()
                        6 -> trimCachesAll()
                        7 -> toggleEnable()
                        8 -> openDetails()
                        9 -> copyPackageName()
                        10 -> showInMarket()
                        11 -> hideAll()
                        12 -> moveToSystem()
                    }
                })
                .show()
    }

    private fun startApp () {
        val intent = this.context.getPackageManager().getLaunchIntentForPackage(app.packageName.toString())
        if (intent != null) {
            if (!app.enabled) {
                enableAll()
            }
            this.context.startActivity(intent)
        }
    }

    /**
     * 显示系统应用选项
     */
    private fun showSystemAppOptions() {
        AlertDialog.Builder(context).setTitle(app.appName)
                .setCancelable(true)
                .setItems(
                        arrayOf("删除",
                                "禁用 + 隐藏",
                                "清空数据",
                                "清除缓存",
                                (if (app.enabled) "冻结" else "解冻"),
                                "应用详情",
                                "复制PackageName",
                                "在应用商店查看"), { _, which ->
                    when (which) {
                        0 -> deleteAll()
                        1 -> hideAll()
                        2 -> clearAll()
                        3 -> trimCachesAll()
                        4 -> toggleEnable()
                        5 -> openDetails()
                        6 -> copyPackageName()
                        7 -> showInMarket()
                    }
                })
                .show()
    }

    /**
     * 显示备份的应用选项
     */
    private fun showBackupAppOptions() {
        AlertDialog.Builder(context).setTitle(app.appName)
                .setCancelable(true)
                .setItems(
                        arrayOf("删除备份",
                                "还原 应用（仅安装）",
                                "还原 应用和数据",
                                "还原 数据",
                                "复制PackageName",
                                "在应用商店查看"), { _, which ->
                    when (which) {
                        0 -> deleteBackupAll()
                        1 -> restoreAll(true, false)
                        2 -> restoreAll(true, true)
                        3 -> restoreAll(false, true)
                        4 -> copyPackageName()
                        5 -> showInMarket()
                    }
                })
                .show()
    }

    private fun toggleEnable() {
        if (app.enabled) {
            disableAll()
        } else {
            enableAll()
        }
    }

    private fun openDetails() {
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
        intent.data = Uri.fromParts("package", app.packageName.toString(), null)
        context.startActivity(intent)
    }

    private fun copyPackageName() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setText(app.packageName)
        Toast.makeText(context, "已复制：${app.packageName}", Toast.LENGTH_LONG).show()
    }

    private fun showInMarket() {
        val str = "market://details?id=" + app.packageName
        val localIntent = Intent("android.intent.action.VIEW")
        localIntent.data = Uri.parse(str)
        context.startActivity(localIntent)
    }

    private fun moveToSystemExec() {
        val sb = StringBuilder()
        sb.append(CommonCmds.MountSystemRW)
        val appDir = File(app.path.toString()).parent
        if (appDir == "/data/app") {
            val parent = File(app.path.toString())
            val outPutPath = "/system/app/${parent.name}"
            sb.append("cp '${app.path}' '$outPutPath'\n")
            sb.append("chmod 0755 '$outPutPath'\n")
            sb.append("chown -R system:system '$outPutPath'\n")
            sb.append("busybox chown -R system:system '$outPutPath'\n")
            sb.append("if [[ ! -e '$outPutPath' ]]; then exit 1; else rm -f '${app.path}'; fi;\n")
        } else {
            val parent = File(appDir)
            val outPutPath = "/system/app/${parent.name}"
            sb.append("cp -pdrf '$appDir' '/system/app/'\n")
            // sb.append("cp -a '$appDir' '$outPutPath'\n")
            sb.append("chmod -R 0755 '$outPutPath'\n")
            sb.append("chown -R system:system '$outPutPath'\n")
            sb.append("busybox chown -R system:system '$outPutPath'\n")
            sb.append("if [[ ! -e '$outPutPath' ]]; then exit 1; exit 1; else rm -rf '$appDir'; fi;\n")
        }
        sb.append("sync;")
        sb.append("sleep 1;")
        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    private fun moveToSystemMagisk() {
        val appDir = File(app.path.toString()).parent
        var result = false
        if (appDir == "/data/app") { // /data/app/xxx.apk
            val parent = File(app.path.toString())
            val outPutPath = "/system/app/"
            result = MagiskExtend.createFileReplaceModule(outPutPath, app.path.toString(), app.packageName.toString(), app.appName.toString()); // MagiskExtend.replaceSystemDir(outPutPath, app.path.toString()) || MagiskExtend.createFileReplaceModule(outPutPath, app.path.toString(), app.packageName.toString());
        } else { // /data/app/xxx.xxx.xxx/xxx.apk
            val parent = File(appDir)
            val outPutPath = "/system/app/" + app.packageName
            result = MagiskExtend.createFileReplaceModule(outPutPath, appDir, app.packageName.toString(), app.appName.toString()); // MagiskExtend.replaceSystemDir(outPutPath, appDir) || MagiskExtend.createFileReplaceModule(outPutPath, appDir, app.packageName.toString());
        }
        if (result) {
            Toast.makeText(context, "已通过Magisk完成操作，请重启手机~", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Magisk镜像空间不足，操作失败！~", Toast.LENGTH_LONG).show()
        }
    }

    private fun moveToSystem() {
        if (CheckRootStatus.isMagisk() && CheckRootStatus.isTmpfs("/system/app") && !MagiskExtend.moduleInstalled()) {
            android.support.v7.app.AlertDialog.Builder(context)
                    .setTitle("Magisk 副作用警告")
                    .setMessage("检测到你正在使用Magisk，并使用了一些会添加系统应用的模块，这导致/system/app被Magisk劫持并且无法写入！！")
                    .setPositiveButton(R.string.btn_confirm, { _, _ ->
                    })
                    .create()
                    .show()
            return
        }
        AlertDialog.Builder(context)
                .setTitle(app.appName)
                .setMessage("转为系统应用后，将无法随意更新或卸载，并且可能会一直后台运行占用内存，继续转换吗？\n\n并非所有应用都可以转换为系统应用，有些转为系统应用后不能正常运行。\n\n确保你已解锁System分区或安装Magisk，转换完成后，请重启手机！")
                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                    if (MagiskExtend.magiskSupported()) {
                        moveToSystemMagisk()
                    } else {
                        moveToSystemExec()
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .setCancelable(true)
                .create()
                .show()
    }
}
