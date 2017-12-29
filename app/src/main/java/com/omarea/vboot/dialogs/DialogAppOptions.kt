package com.omarea.vboot.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shell.AsynSuShellUnit
import com.omarea.shell.SysUtils
import com.omarea.vboot.R
import java.io.File
import java.util.*

/**
 * Created by helloklf on 2017/12/04.
 */

class DialogAppOptions(private var context: Context, private var apps: ArrayList<HashMap<String, Any>>, private var handler: Handler) {
    private var allowPigz = false
    private var backupPath = Consts.AbsBackUpDir

    fun selectUserAppOptions() {
        AlertDialog.Builder(context).setTitle("请选择操作")
                .setCancelable(true)
                .setItems(
                        arrayOf("备份（apk、data）",
                                "备份（apk）",
                                "卸载",
                                "卸载（保留数据）",
                                "清空数据",
                                "清除缓存",
                                "冻结",
                                "解冻"), { _, which ->
                    when (which) {
                        0 -> {
                            if (!checkRestoreData()) {
                                Toast.makeText(context, "抱歉，数据备份还原功能暂不支持你的设备！", Toast.LENGTH_LONG).show()
                                return@setItems
                            }
                            confirm("备份应用和数据", "备份功能目前还是实验性的，无法保证在所有设备上运行，备份可能无法正常还原。继续尝试使用吗？", Runnable {
                                backupAll(true, true)
                            })
                        }
                        1 -> {
                            backupAll(true, false)
                        }
                        2 -> {
                            confirm("卸载", "正在卸载${apps.size}个应用，继续吗？", Runnable {
                                uninstallAll()
                            })
                        }
                        3 -> {
                            confirm("卸载（保留数据）", "正在卸载${apps.size}个应用，这些应用的数据会被保留，这可能会导致下次安装不同签名的同名应用时无法安装，继续吗？", Runnable {
                                uninstallKeepDataAll()
                            })
                        }
                        4 -> {
                            confirm("清除应用数据", "正在清除${apps.size}个应用的数据，继续吗？", Runnable {
                                clearAll()
                            })
                        }
                        5 -> trimCachesAll()
                        6 -> disableAll()
                        7 -> enableAll()
                    }
                })
                .show()
    }

    fun selectSystemAppOptions() {
        AlertDialog.Builder(context).setTitle("请选择操作")
                .setCancelable(true)
                .setItems(
                        arrayOf("删除",
                                "清空数据",
                                "清除缓存",
                                "冻结",
                                "解冻",
                                "禁用+隐藏"), { _, which ->
                    when (which) {
                        0 -> {
                            confirm("删除应用", "删除系统应用可能导致功能不正常，甚至无法开机，确定要继续删除？", Runnable {
                                deleteAll()
                            })
                        }
                        1 -> {
                            confirm("清空应用数据", "已选中了${apps.size}个应用，这些应用的数据将会被清除，确定吗？", Runnable {
                                clearAll()
                            })
                        }
                        2 -> trimCachesAll()
                        3 -> {
                            confirm("冻结应用", "已选中了${apps.size}个应用，这些应用将会被冻结，可能导致手机功能不正常，继续冻结？", Runnable {
                                disableAll()
                            })
                        }
                        4 -> enableAll()
                        5 -> {
                            confirm("隐藏应用", "你将禁用并隐藏${apps.size}个应用，该操作无法撤销，继续这个操作？", Runnable {
                                hideAll()
                            })
                        }
                    }
                })
                .show()
    }

    fun selectBackupOptions() {
        AlertDialog.Builder(context).setTitle("请选择操作")
                .setCancelable(true)
                .setItems(
                        arrayOf("删除备份",
                                "还原",
                                "还原(应用)",
                                "还原(数据)"), { _, which ->
                    when (which) {
                        0 -> {
                            confirm("删除备份", "永久删除这些备份文件？", Runnable {
                                deleteBackupAll()
                            })
                        }
                        1 -> {
                            if (!checkRestoreData()) {
                                Toast.makeText(context, "抱歉，数据备份还原功能暂不支持你的设备！", Toast.LENGTH_LONG).show()
                                return@setItems
                            }
                            confirm("还原应用和数据", "该功能目前还在实验阶段，可能不能在所有设备上正常运行，也许会导致应用数据丢失。\n继续尝试恢复吗？", Runnable {
                                restoreAll(true, true)
                            })
                        }
                        2 -> {
                            confirm("还原应用", "该功能目前还在实验阶段，可能不能在所有设备上正常运行，也许会导致应用数据丢失。\n继续尝试恢复吗？", Runnable {
                                restoreAll(true, false)
                            })
                        }
                        3 -> {
                            if (!checkRestoreData()) {
                                Toast.makeText(context, "抱歉，数据备份还原功能暂不支持你的设备！", Toast.LENGTH_LONG).show()
                                return@setItems
                            }
                            confirm("还原数据", "该功能目前还在实验阶段，可能不能在所有设备上正常运行，也许会导致应用数据丢失。\n继续尝试恢复吗？", Runnable {
                                restoreAll(false, true)
                            })
                        }
                    }
                })
                .show()
    }

    private fun checkRestoreData(): Boolean {
        var r = SysUtils.executeCommandWithOutput(false, "cd /data/data/${Consts.PACKAGE_NAME};echo `toybox ls -ld|cut -f3 -d ' '`; echo `ls -ld|cut -f3 -d ' '`;")
        return r != null && r.trim().length > 0
    }

    private fun execShell(sb: StringBuilder) {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_app_options, null)
        val textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
        textView.text = "正在获取权限"
        val alert = AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
        AsynSuShellUnit(ProgressHandler(dialog, alert, handler)).exec(sb.toString()).waitFor()
        alert.show()
    }

    class ProgressHandler(dialog: View, private var alert: AlertDialog, private var handler: Handler) : Handler() {
        private var textView: TextView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
        var progressBar: ProgressBar = (dialog.findViewById(R.id.dialog_app_details_progress) as ProgressBar)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.obj != null) {
                if (msg.what == 0) {
                    textView.text = "正在执行操作..."
                } else {
                    val obj = msg.obj.toString()
                    if (obj == "[operation completed]") {
                        progressBar.progress = 100
                        textView.text = "操作完成！"
                        handler.postDelayed({
                            alert.dismiss()
                            alert.hide()
                        }, 2000)
                        handler.handleMessage(handler.obtainMessage(2))
                    } else if (Regex("^\\[.*\\]\$").matches(obj)) {
                        progressBar.progress = msg.what
                        val txt = obj
                                .replace("[copy ", "[复制 ")
                                .replace("[uninstall ", "[卸载 ")
                                .replace("[install ", "[安装 ")
                                .replace("[restore ", "[还原 ")
                                .replace("[backup ", "[备份 ")
                                .replace("[unhide ", "[显示 ")
                                .replace("[hide ", "[隐藏 ")
                                .replace("[delete ", "[删除 ")
                                .replace("[disable ", "[禁用 ")
                                .replace("[enable ", "[启用 ")
                                .replace("[trim caches ", "[清除缓存 ")
                                .replace("[clear ", "[清除数据 ")
                                .replace("[skip ", "[跳过 ")
                                .replace("[link ", "[链接 ")
                        textView.text = txt
                    }
                }
            }
        }

        init {
            textView.text = "正在获取权限"
        }
    }

    private fun confirm(title: String, msg: String, next: Runnable?) {
        AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton("确定", { _, _ ->
                    next?.run()
                })
                .setNeutralButton("取消", { _, _ ->
                })
                .create()
                .show()
    }

    /**
     * 检查是否可用pigz
     */
    private fun checkPigz() {
        if (File("/system/xbin/pigz").exists() || File("/system/bin/pigz").exists()) {
            allowPigz = true
        }
    }

    /**
     * 备份选中的应用
     */
    private fun backupAll(apk: Boolean = true, data: Boolean = true) {
        checkPigz()

        val date = Date().time.toString()

        val sb = StringBuilder()
        sb.append("backup_date=\"$date\";");
        sb.append("\n")
        sb.append("backup_path=\"${Consts.AbsBackUpDir}\";")
        sb.append("\n")
        sb.append("hard_link_path=\"${Consts.HarLinkBackUpDir}\";")
        sb.append("mkdir -p \${backup_path};")
        sb.append("\n")
        sb.append("echo \$backup_date > \${backup_path}date.dat;")
        sb.append("\n")
        sb.append("if [ `cat \${backup_path}date.dat` = \$backup_date ];")
        sb.append(" then ")
            sb.append("backup_path=\${hard_link_path};")
        sb.append(" fi;")
        sb.append("\n")

        for (item in apps) {
            val packageName = item["packageName"].toString()
            val path = item["path"].toString()

            if (apk) {
                sb.append("rm -f \${backup_path}$packageName.apk;")
                sb.append("\n")
                /*
                sb.append("ln -f $path \${backup_path}$packageName.apk;")
                sb.append("\n")
                sb.append("if [ -f \${backup_path}$packageName.apk ];")
                sb.append(" then ")
                    sb.append("echo '[link $packageName.apk]';")
                    sb.append("sleep 1;")
                sb.append(" else ")
                    sb.append("echo '[copy $packageName.apk]';")
                    sb.append("cp -F $path \${backup_path}$packageName.apk;")
                sb.append("fi;")
                */
                sb.append("echo '[copy $packageName.apk]';")
                sb.append("cp -F $path \${backup_path}$packageName.apk;")
                sb.append("\n")
            }
            if (data) {
                sb.append("cd /data/data/$packageName;")
                sb.append("echo '[backup $packageName]';")
                if (allowPigz)
                    sb.append("busybox tar cpf - * --exclude cache --exclude lib | pigz > \${backup_path}$packageName.tar.gz;")
                else
                    sb.append("busybox tar -czpf \${backup_path}$packageName.tar.gz * --exclude cache --exclude lib;")
                sb.append("\n")
            }
        }
        sb.append("cd \${backup_path};")
        sb.append("chown sdcard_rw:sdcard_rw *;")
        sb.append("chmod 777 *;")
        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 还原选中的应用
     */
    private fun restoreAll(apk: Boolean = true, data: Boolean = true) {
        checkPigz()

        val sb = StringBuilder()
        sb.append("chown sdcard_rw *;")
        sb.append("chmod 7777 *;")
        for (item in apps) {
            val packageName = item["packageName"].toString()
            val apkPath = item["path"].toString()
            if (apk && File("$backupPath$packageName.apk").exists()) {
                sb.append("echo '[install $packageName]';")

                sb.append("pm install $backupPath$packageName.apk;")
            } else if (apk && File(apkPath).exists()) {
                sb.append("echo '[install $packageName]';")

                sb.append("pm install $apkPath;")
            }
            if (data && File("$backupPath$packageName.tar.gz").exists()) {
                sb.append("if [ -d /data/data/$packageName ];")
                sb.append(" then ")
                    sb.append("echo '[restore $packageName]';")
                    sb.append("pm clear $packageName;")
                    sb.append("cd /data/data/$packageName;")
                    sb.append("busybox tar -xzpf $backupPath$packageName.tar.gz;")
                    sb.append("chown -R -L `toybox ls -ld .|cut -f3 -d ' '`:`toybox ls -ld .|cut -f4 -d ' '` *;")
                    //sb.append("chown -R --reference=/data/data/$packageName *;")
                sb.append(" else ")
                    sb.append("echo '[skip $packageName]';")
                    sb.append("sleep 1;")
                sb.append("fi;")
            }
        }
        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 禁用所选的应用
     */
    private fun disableAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[disable $packageName]';")

            sb.append("pm disable $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 启用所选的应用
     */
    private fun enableAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[enable $packageName]';")

            sb.append("pm enable $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 隐藏所选的应用
     */
    private fun hideAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[hide $packageName]';")

            sb.append("pm hide $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 删除选中的应用
     */
    private fun deleteAll() {
        val sb = StringBuilder()
        sb.append(Consts.MountSystemRW)
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[delete $packageName]';")

            val dir = item["dir"].toString()
            sb.append("rm -rf $dir;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 删除备份
     */
    private fun deleteBackupAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[delete $packageName]';")

            sb.append("rm -rf $backupPath$packageName.apk;")
            sb.append("rm -rf $backupPath$packageName.tar.gz;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 清除数据
     */
    private fun clearAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[clear $packageName]';")

            sb.append("pm clear $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 清除缓存
     */
    private fun trimCachesAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[trim caches $packageName]';")

            sb.append("pm trim-caches $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 卸载选中
     */
    private fun uninstallAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[uninstall $packageName]';")

            sb.append("pm uninstall $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 卸载且保留数据
     */
    private fun uninstallKeepDataAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item["packageName"].toString()
            sb.append("echo '[uninstall $packageName]';")

            sb.append("pm uninstall -k $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }
}
