package com.omarea.vboot.dialogs

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.widget.Toast
import com.omarea.shared.model.Appinfo

/**
 * Created by Hello on 2018/01/26.
 */

class DialogSingleAppOptions(override var context: Context, private var app: Appinfo, override var handler: Handler) : DialogAppOptions(context, arrayListOf<Appinfo>(app), handler) {
    /**
     * 显示用户应用选项
     */
    fun showUserAppOptions() {
        AlertDialog.Builder(context).setTitle(app.appName)
                .setCancelable(true)
                .setItems(
                        arrayOf("备份（apk、data）",
                                "备份（apk）",
                                "卸载",
                                "卸载（保留数据）",
                                "清空数据",
                                "清除缓存",
                                (if(app.enabled)  "冻结" else "解冻"),
                                "应用详情",
                                "复制PackageName",
                                "在应用商店查看"), { _, which ->
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
                    }
                })
                .show()
    }

    /**
     * 显示系统应用选项
     */
    fun showSystemAppOptions() {
        AlertDialog.Builder(context).setTitle(app.appName)
                .setCancelable(true)
                .setItems(
                        arrayOf("删除",
                                "禁用 + 隐藏",
                                "清空数据",
                                "清除缓存",
                                (if(app.enabled)  "冻结" else "解冻"),
                                "应用详情",
                                "复制PackageName",
                                "在应用商店查看"), { _, which ->
                    when (which) {
                        0 -> uninstallAll()
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
    fun showBackupAppOptions() {
        AlertDialog.Builder(context).setTitle(app.appName)
                .setCancelable(true)
                .setItems(
                        arrayOf("删除备份",
                                "还原 应用（仅安装）",
                                "还原 应用和数据",
                                "还原 数据",
                                "复制包名",
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
}
