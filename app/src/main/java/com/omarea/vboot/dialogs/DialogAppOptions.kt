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
import com.omarea.shared.model.Appinfo
import com.omarea.shell.AsynSuShellUnit
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.SysUtils
import com.omarea.vboot.R
import java.io.File
import java.util.*

/**
 * Created by helloklf on 2017/12/04.
 */

open class DialogAppOptions(protected final var context: Context, protected var apps: ArrayList<Appinfo>, protected var handler: Handler) {
    protected var allowPigz = false
    protected var backupPath = Consts.AbsBackUpDir
    protected var userdataPath = ""

    init {
        userdataPath = context.filesDir.absolutePath
        userdataPath = userdataPath.substring(0, userdataPath.indexOf(context.packageName) - 1)
    }

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
                        0 -> backupAll(true, true)
                        1 -> backupAll(true, false)
                        2 -> uninstallAll()
                        3 -> uninstallKeepDataAll()
                        4 -> clearAll()
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
                        0 -> deleteAll()
                        1 -> clearAll()
                        2 -> trimCachesAll()
                        3 -> disableAll()
                        4 -> enableAll()
                        5 -> hideAll()
                    }
                })
                .show()
    }

    fun selectBackupOptions() {
        AlertDialog
                .Builder(context)
                .setTitle("请选择操作")
                .setCancelable(true)
                .setItems(arrayOf("删除备份", "还原", "还原(应用)", "还原(数据)"), { _, which ->
                    when (which) {
                        0 -> deleteBackupAll()
                        1 -> restoreAll(true, true)
                        2 -> restoreAll(true, false)
                        3 -> restoreAll(false, true)
                    }
                })
                .show()
    }

    protected fun checkRestoreData(): Boolean {
        val r = SysUtils.executeCommandWithOutput(false, "cd $userdataPath/${Consts.PACKAGE_NAME};echo `toybox ls -ld|cut -f3 -d ' '`; echo `ls -ld|cut -f3 -d ' '`;")
        return r != null && r.trim().length > 0
    }

    protected fun execShell(sb: StringBuilder) {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_app_options, null)
        val textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
        textView.text = "正在获取权限"
        val alert = AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
        AsynSuShellUnit(ProgressHandler(dialog, alert, handler)).exec(sb.toString()).waitFor()
        alert.show()
    }

    class ProgressHandler(dialog: View, protected var alert: AlertDialog, protected var handler: Handler) : Handler() {
        protected var textView: TextView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
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

    protected fun confirm(title: String, msg: String, next: Runnable?) {
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
    protected fun checkPigz() {
        if (File("/system/xbin/pigz").exists() || File("/system/bin/pigz").exists()) {
            allowPigz = true
        }
    }

    /**
     * 备份选中的应用
     */
    protected fun backupAll(apk: Boolean = true, data: Boolean = true) {
        if (data) {
            if (!checkRestoreData()) {
                Toast.makeText(context, "抱歉，数据备份还原功能暂不支持你的设备！", Toast.LENGTH_LONG).show()
                return
            }
            confirm("备份应用和数据", "备份所选的${apps.size}个应用和数据？（很不推荐使用数据备份功能，因为经常会有兼容性问题，可能导致还原的软件出现FC并出现异常耗电）", Runnable {
                _backupAll(apk, data)
            })
        } else {
            _backupAll(apk, data)
        }
    }

    private fun _backupAll(apk: Boolean = true, data: Boolean = true) {
        checkPigz()

        val date = Date().time.toString()

        val sb = StringBuilder()
        sb.append("backup_date=\"$date\";");
        sb.append("\n")
        sb.append("backup_path=\"${Consts.AbsBackUpDir}\";")
        sb.append("mkdir -p \${backup_path};")
        sb.append("\n")
        sb.append("\n")

        for (item in apps) {
            val packageName = item.packageName.toString()
            val path = item.path.toString()

            if (apk) {
                sb.append("rm -f \${backup_path}$packageName.apk;")
                sb.append("\n")
                sb.append("echo '[copy $packageName.apk]';")
                sb.append("cp -F $path \${backup_path}$packageName.apk;")
                sb.append("\n")
            }
            if (data) {
                sb.append("killall -9 $packageName;pkill -9 $packageName;pgrep $packageName |xargs kill -9;")
                sb.append("cd $userdataPath/$packageName;")
                sb.append("echo '[backup $packageName]';")
                if (allowPigz)
                    sb.append("busybox tar cpf - * --exclude ./cache --exclude ./lib | pigz > \${backup_path}$packageName.tar.gz;")
                else
                    sb.append("busybox tar -czpf \${backup_path}$packageName.tar.gz * --exclude ./cache --exclude ./lib;")
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
    protected fun restoreAll(apk: Boolean = true, data: Boolean = true) {
        if (data) {
            if (!checkRestoreData()) {
                Toast.makeText(context, "抱歉，数据备份还原功能暂不支持你的设备！", Toast.LENGTH_LONG).show()
                return
            }
            confirm("还原应用和数据", "还原所选的${apps.size}个应用和数据？（很不推荐使用数据还原功能，因为经常会有兼容性问题，可能导致还原的软件出现FC并出现异常耗电）", Runnable {
                _restoreAll(apk, data)
            })
        } else {
            confirm("还原应用", "还原所选的${apps.size}个应用和数据？", Runnable {
                _restoreAll(apk, data)
            })
        }
    }

    private fun _restoreAll(apk: Boolean = true, data: Boolean = true) {
        checkPigz()

        val sb = StringBuilder()
        sb.append("chown sdcard_rw *;")
        sb.append("chmod 7777 *;")
        for (item in apps) {
            val packageName = item.packageName.toString()
            val apkPath = item.path.toString()
            if (apk && File("$backupPath$packageName.apk").exists()) {
                sb.append("echo '[install $packageName]';")

                sb.append("pm install -r $backupPath$packageName.apk;")
            } else if (apk && File(apkPath).exists()) {
                sb.append("echo '[install $packageName]';")

                sb.append("pm install -r $apkPath;")
            }
            if (data && File("$backupPath$packageName.tar.gz").exists()) {
                sb.append("if [ -d $userdataPath/$packageName ];")
                sb.append(" then ")
                sb.append("echo '[restore $packageName]';")
                //sb.append("pm clear $packageName;")
                sb.append("sync;")
                sb.append("cd $userdataPath/$packageName;")
                sb.append("busybox tar -xzpf $backupPath$packageName.tar.gz;")
                sb.append("chown -R -L `toybox ls -ld|cut -f3 -d ' '`:`toybox ls -ld|cut -f4 -d ' '` $userdataPath/$packageName/*;")
                //sb.append("chown -R --reference=$userdataPath/$packageName *;")
                sb.append(" else ")
                sb.append("echo '[skip $packageName]';")
                sb.append("sleep 1;")
                sb.append("fi;")
            }
        }
        sb.append("sync;")
        sb.append("sleep 2;")
        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 禁用所选的应用
     */
    protected fun disableAll() {
        confirm("冻结应用", "已选中了${apps.size}个应用，这些应用将会被冻结，可能导致手机功能不正常，继续冻结？", Runnable {
            _disableAll()
        })
    }

    private fun _disableAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[disable $packageName]';")

            sb.append("pm disable $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 启用所选的应用
     */
    protected fun enableAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[enable $packageName]';")

            sb.append("pm enable $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 隐藏所选的应用
     */
    protected fun hideAll() {
        confirm("隐藏应用", "你将禁用并隐藏${apps.size}个应用，该操作无法撤销，继续这个操作？", Runnable {
            _hideAll()
        })
    }

    private fun _hideAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[hide $packageName]';")

            sb.append("pm hide $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 删除选中的应用
     */
    protected fun deleteAll() {
        confirm("删除应用", "删除系统应用可能导致功能不正常，甚至无法开机，确定要继续删除？", Runnable {
            if (CheckRootStatus.isMagisk() && (CheckRootStatus.isTmpfs("/system/app") || CheckRootStatus.isTmpfs("/system/priv-app"))) {
                android.support.v7.app.AlertDialog.Builder(context)
                        .setTitle("Magisk 副作用警告")
                        .setMessage("检测到你正在使用Magisk作为ROOT权限管理器，并且/system/app和/system/priv-app目录已被某些模块修改，这可能导致这些目录被Magisk覆盖并且无法写入！！")
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            _deleteAll()
                        })
                        .create()
                        .show()
            } else {
                _deleteAll()
            }
        })
    }

    private fun _deleteAll() {
        val sb = StringBuilder()
        sb.append(Consts.MountSystemRW)
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[delete $packageName]';")

            val dir = item.dir.toString()

            sb.append("rm -rf $dir/oat;")
            sb.append("rm -rf $dir/lib;")
            sb.append("rm -rf ${item.path};")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 删除备份
     */
    protected fun deleteBackupAll() {
        confirm("删除备份", "永久删除这些备份文件？", Runnable {
            _deleteBackupAll()
        })
    }

    private fun _deleteBackupAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[delete $packageName]';")

            if (item.path != null) {
                sb.append("rm -rf ${item.path};")
                if (item.path == "$backupPath$packageName.apk") {
                    sb.append("rm -rf $backupPath$packageName.tar.gz;")
                }
            } else {
                sb.append("rm -rf $backupPath$packageName.apk;")
                sb.append("rm -rf $backupPath$packageName.tar.gz;")
            }
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 清除数据
     */
    protected fun clearAll() {
        confirm("清空应用数据", "已选中了${apps.size}个应用，这些应用的数据将会被清除，确定吗？", Runnable {
            _clearAll()
        })
    }

    private fun _clearAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[clear $packageName]';")

            sb.append("pm clear $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 清除缓存
     */
    protected fun trimCachesAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[trim caches $packageName]';")

            sb.append("pm trim-caches $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 卸载选中
     */
    protected fun uninstallAll() {
        confirm("卸载", "正在卸载${apps.size}个应用，继续吗？", Runnable {
            _uninstallAll()
        })
    }

    private fun _uninstallAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[uninstall $packageName]';")

            sb.append("pm uninstall $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 卸载且保留数据
     */
    protected fun uninstallKeepDataAll() {
        confirm("卸载（保留数据）", "正在卸载${apps.size}个应用，这些应用的数据会被保留，这可能会导致下次安装不同签名的同名应用时无法安装，继续吗？", Runnable {
            _uninstallKeepDataAll()
        })
    }

    private fun _uninstallKeepDataAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[uninstall $packageName]';")

            sb.append("pm uninstall -k $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }
}
