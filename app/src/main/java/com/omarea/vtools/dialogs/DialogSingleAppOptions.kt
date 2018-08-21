package com.omarea.vtools.dialogs

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.shared.model.Appinfo
import com.omarea.shell.CheckRootStatus
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.dpi_input.view.*
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
                        arrayOf("备份（apk、data）",
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
                        0 -> backupAll(true, true)
                        1 -> backupAll(true, false)
                        2 -> uninstallAll()
                        3 -> uninstallKeepDataAll()
                        4 -> clearAll()
                        5 -> trimCachesAll()
                        6 -> toggleEnable()
                        7 -> openDetails()
                        8 -> copyPackageName()
                        9 -> showInMarket()
                        10 -> hideAll()
                        11 -> moveToSystem()
                    }
                })
                .show()
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
                        1 -> uninstallKeepDataAll()
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

    private fun moveToSystemExec () {
        val sb = StringBuilder()
        sb.append(CommonCmds.MountSystemRW)
        val appDir = File(app.path.toString()).parent
        if ( appDir == "/data/app") {
            val parent = File(app.path.toString())
            val outPutPath = "/system/app/${parent.name}"
            sb.append("cp '${app.path}' '$outPutPath'\n")
            sb.append("chmod 0664 '$outPutPath'\n")
            sb.append("chown -R system:system '$outPutPath'\n")
            sb.append("busybox chown -R system:system '$outPutPath'\n")
            sb.append("if [[ ! -e '$outPutPath' ]]; then exit 1; else rm -f '${app.path}'; fi;\n")
        } else {
            val parent = File(appDir)
            val outPutPath = "/system/app/${parent.name}"
            sb.append("cp -a '$appDir' '$outPutPath'\n")
            sb.append("chmod -R 0664 '$outPutPath'\n")
            sb.append("chown -R system:system '$outPutPath'\n")
            sb.append("busybox chown -R system:system '$outPutPath'\n")
            sb.append("if [[ ! -e '$outPutPath' ]]; then exit 1; exit 1; else rm -rf '$appDir'; fi;\n")
        }
        sb.append("sync;")
        sb.append("sleep 1;")
        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    private fun moveToSystem () {
        if (CheckRootStatus.isMagisk() && CheckRootStatus.isTmpfs("/system/app")) {
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
                .setMessage("转为系统应用后，将无法随意更新或卸载，并且可能会一直后台运行占用内存，继续转换吗？\n\n并非所有应用都可以转换为系统应用，有些转为系统应用后不能正常运行。\n\n确保你已解锁System分区，转换完成后，请重启手机！")
                .setPositiveButton(R.string.btn_confirm, {
                    _, _ ->
                    moveToSystemExec()
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .setCancelable(true)
                .create()
                .show()
    }
}
