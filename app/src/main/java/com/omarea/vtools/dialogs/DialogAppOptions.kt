package com.omarea.vtools.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.UserManager
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.shell.AsynSuShellUnit
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.model.Appinfo
import com.omarea.permissions.CheckRootStatus
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import java.io.File
import java.util.*


/**
 * Created by helloklf on 2017/12/04.
 */

open class DialogAppOptions(protected final var context: Context, protected var apps: ArrayList<Appinfo>, protected var handler: Handler) {
    protected var allowPigz = false
    protected var backupPath = CommonCmds.AbsBackUpDir
    protected var userdataPath = ""

    init {
        userdataPath = context.filesDir.absolutePath
        userdataPath = userdataPath.substring(0, userdataPath.indexOf(context.packageName) - 1)
    }

    fun selectUserAppOptions(activity: Activity) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_app_options_user, null)

        val dialog = DialogHelper.animDialog(AlertDialog.Builder(context)
                .setCancelable(true)
                .setView(dialogView))
        dialogView.findViewById<View>(R.id.app_options_single_only).visibility = View.GONE
        dialogView.findViewById<View>(R.id.app_options_app_hide).setOnClickListener {
            dialog?.dismiss()
            hideAll()
        }
        dialogView.findViewById<View>(R.id.app_options_trim).setOnClickListener {
            dialog?.dismiss()
            trimCachesAll()
        }
        dialogView.findViewById<View>(R.id.app_options_clear).setOnClickListener {
            dialog?.dismiss()
            clearAll()
        }
        dialogView.findViewById<View>(R.id.app_options_backup_apk).setOnClickListener {
            dialog?.dismiss()
            backupAll(true, false)
        }
        dialogView.findViewById<View>(R.id.app_options_backup_all).setOnClickListener {
            dialog?.dismiss()
            backupAll(true, true)
        }
        dialogView.findViewById<View>(R.id.app_options_uninstall).setOnClickListener {
            dialog?.dismiss()
            uninstallAll()
        }
        dialogView.findViewById<View>(R.id.app_options_uninstall_user).setOnClickListener {
            dialog?.dismiss()
            uninstallAllOnlyUser()
        }
        /*
        dialogView.findViewById<View>(R.id.app_options_as_system).setOnClickListener {
            dialog?.dismiss()
            moveToSystem()
        }
        */
        dialogView.findViewById<View>(R.id.app_options_dex2oat).setOnClickListener {
            dialog?.dismiss()
            buildAll()
        }
        dialogView.findViewById<View>(R.id.app_options_uninstall_keep).setOnClickListener {
            dialog?.dismiss()
            uninstallKeepDataAll()
        }
        dialogView.findViewById<TextView>(R.id.app_options_title).text = "请选择操作"

        if (apps.any { it.enabled }) {
            dialogView.findViewById<View>(R.id.app_options_app_unfreeze).visibility = View.GONE
            dialogView.findViewById<View>(R.id.app_options_app_freeze).setOnClickListener {
                dialog?.dismiss()
                disableAll()
            }
        } else {
            dialogView.findViewById<View>(R.id.app_options_app_freeze).visibility = View.GONE
            dialogView.findViewById<View>(R.id.app_options_app_unfreeze).setOnClickListener {
                dialog?.dismiss()
                enableAll()
            }
        }
    }

    fun selectSystemAppOptions(activity: Activity) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_app_options_system, null)

        val dialog = DialogHelper.animDialog(AlertDialog.Builder(context)
                .setCancelable(true)
                .setView(dialogView))
        dialogView.findViewById<View>(R.id.app_options_single_only).visibility = View.GONE
        dialogView.findViewById<View>(R.id.app_options_app_hide).setOnClickListener {
            dialog?.dismiss()
            hideAll()
        }
        dialogView.findViewById<View>(R.id.app_options_trim).setOnClickListener {
            dialog?.dismiss()
            trimCachesAll()
        }
        dialogView.findViewById<View>(R.id.app_options_clear).setOnClickListener {
            dialog?.dismiss()
            clearAll()
        }
        dialogView.findViewById<View>(R.id.app_options_uninstall_user).setOnClickListener {
            dialog?.dismiss()
            uninstallAllOnlyUser()
        }
        dialogView.findViewById<View>(R.id.app_options_dex2oat).setOnClickListener {
            dialog?.dismiss()
            buildAll()
        }

        dialogView.findViewById<View>(R.id.app_options_delete).setOnClickListener {
            dialog?.dismiss()
            deleteAll()
        }
        dialogView.findViewById<View>(R.id.app_options_uninstall).visibility = View.GONE

        dialogView.findViewById<TextView>(R.id.app_options_title).setText("请选择操作")

        dialogView.findViewById<View>(R.id.app_options_app_unfreeze).setOnClickListener {
            dialog?.dismiss()
            enableAll()
        }
        dialogView.findViewById<View>(R.id.app_options_app_freeze).setOnClickListener {
            dialog?.dismiss()
            disableAll()
        }
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
        val r = KeepShellPublic.doCmdSync("cd $userdataPath/${context.packageName};echo `toybox ls -ld|cut -f3 -d ' '`; echo `ls -ld|cut -f3 -d ' '`;")
        return r != "error" && r.trim().isNotEmpty()
    }

    protected fun execShell(sb: StringBuilder) {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_loading, null)
        val textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
        textView.text = "正在获取权限"
        val alert = AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
        AsynSuShellUnit(ProgressHandler(dialog, alert, handler)).exec(sb.toString()).waitFor()
        alert.show()
    }

    open class ProgressHandler(dialog: View, protected var alert: AlertDialog, protected var handler: Handler) : Handler() {
        protected var textView: TextView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
        var progressBar: ProgressBar = (dialog.findViewById(R.id.dialog_app_details_progress) as ProgressBar)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.obj != null) {
                if (msg.what == 0) {
                    textView.text = "正在执行操作..."
                } else {
                    val obj = msg.obj.toString()
                    if (obj.contains("[operation completed]")) {
                        progressBar.progress = 100
                        textView.text = "操作完成！"
                        handler.postDelayed({
                            alert.dismiss()
                            alert.hide()
                        }, 2000)
                        handler.handleMessage(handler.obtainMessage(2))
                    } else if (Regex("^\\[.*]\$").matches(obj)) {
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
                                .replace("[compile ", "[编译 ")
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
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton("确定", { _, _ ->
                    next?.run()
                })
                .setNeutralButton("取消", { _, _ ->
                }))
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
        sb.append("backup_path=\"${CommonCmds.AbsBackUpDir}\";")
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
                sb.append("echo '[backup ${item.appName}]';")
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
                sb.append("echo '[install ${item.appName}]';")

                sb.append("pm install -r $backupPath$packageName.apk;")
            } else if (apk && File(apkPath).exists()) {
                sb.append("echo '[install ${item.appName}]';")

                sb.append("pm install -r $apkPath;")
            }
            if (data && File("$backupPath$packageName.tar.gz").exists()) {
                sb.append("if [ -d $userdataPath/$packageName ];")
                sb.append(" then ")
                sb.append("echo '[restore ${item.appName}]';")
                //sb.append("pm clear $packageName;")
                sb.append("sync;")
                sb.append("cd $userdataPath/$packageName;")
                sb.append("busybox tar -xzpf $backupPath$packageName.tar.gz;")
                sb.append("chown -R -L `toybox ls -ld|cut -f3 -d ' '`:`toybox ls -ld|cut -f4 -d ' '` $userdataPath/$packageName/*;")
                //sb.append("chown -R --reference=$userdataPath/$packageName *;")
                sb.append(" else ")
                sb.append("echo '[skip ${item.appName}]';")
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
            sb.append("echo '[disable ${item.appName}]';")

            sb.append("pm disable ${packageName};")
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
            sb.append("echo '[enable ${item.appName}]';")

            sb.append("pm enable $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 隐藏所选的应用
     */
    protected fun hideAll() {
        confirm("隐藏应用", "你将禁用并隐藏${apps.size}个应用。\n\n卸载Scene前务必先恢复这些应用，否则你将需要恢复出厂设置才能还原这些应用。\n\n继续隐藏吗操作？", Runnable {
            _hideAll()
        })
    }

    @SuppressLint("ApplySharedPref")
    private fun _hideAll() {
        val spf = context.getSharedPreferences(SpfConfig.APP_HIDE_HISTORY_SPF, Context.MODE_PRIVATE).edit()
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[hide ${item.appName}]';")

            sb.append("pm hide $packageName;")

            spf.putString(packageName, if (item.appName != null) item.appName as String? else packageName)
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
        spf.commit()
    }

    /**
     * 删除选中的应用
     */
    protected fun deleteAll() {
        confirm("删除应用", "已选择${apps.size}个应用，删除系统应用可能导致功能不正常，甚至无法开机，确定要继续删除？", Runnable {
            if (CheckRootStatus.isMagisk() && !com.omarea.common.shared.MagiskExtend.moduleInstalled() && (CheckRootStatus.isTmpfs("/system/app") || CheckRootStatus.isTmpfs("/system/priv-app"))) {
                android.support.v7.app.AlertDialog.Builder(context)
                        .setTitle("Magisk 副作用警告")
                        .setMessage("检测到你正在使用Magisk作为ROOT权限管理器，并且/system/app和/system/priv-app目录已被某些模块修改，这可能导致这些目录被Magisk劫持并且无法写入！！")
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
        sb.append(CommonCmds.MountSystemRW)
        var useMagisk = false
        for (item in apps) {
            val packageName = item.packageName.toString()
            // 先禁用再删除，避免老弹停止运行
            sb.append("echo '[disable ${item.appName}]';")
            sb.append("pm disable $packageName;")

            sb.append("echo '[delete ${item.appName}]'\n")
            if (com.omarea.common.shared.MagiskExtend.moduleInstalled()) {
                com.omarea.common.shared.MagiskExtend.deleteSystemPath(item.path.toString());
                useMagisk = true
            } else {
                val dir = item.dir.toString()

                sb.append("rm -rf $dir/oat\n")
                sb.append("rm -rf $dir/lib\n")
                sb.append("rm -rf ${item.path}\n")
            }
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
        if (useMagisk) {
            Toast.makeText(context, "已通过Magisk更改参数，请重启手机~", Toast.LENGTH_SHORT).show()
        }
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
            sb.append("echo '[delete ${item.appName}]';")

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
        confirm("清空应用数据", "已选中${apps.size}个应用，这些应用的数据将会被清除，确定吗？", Runnable {
            _clearAll()
        })
    }

    private fun _clearAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[clear ${item.appName}]';")

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
            sb.append("echo '[trim caches ${item.appName}]';")

            sb.append("pm trim-caches $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    /**
     * 卸载选中
     */
    protected fun uninstallAll() {
        confirm("彻底卸载", "已选中${apps.size}个应用，正在卸载${apps.size}个应用，继续吗？", Runnable {
            _uninstallAll()
        })
    }

    private fun _uninstallAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[uninstall ${item.appName}]'\n")

            sb.append("pm uninstall $packageName\n")
        }

        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    protected fun uninstallAllOnlyUser() {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager?
        val userHandle = android.os.Process.myUserHandle()
        if (um != null) {
            val uid = um.getSerialNumberForUser(userHandle)
            confirm("从当前用户(" + uid + ")卸载", "已选中${apps.size}个应用，正在卸载${apps.size}个应用，继续吗？", Runnable {
                _uninstallAllOnlyUser(uid)
            })
        } else {
            Toast.makeText(context, "获取用户ID失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun _uninstallAllOnlyUser(uid: Long) {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[uninstall ${item.appName}]'\n")

            sb.append("pm uninstall --user $uid $packageName\n")
        }

        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    /**
     * 卸载且保留数据
     */
    protected fun uninstallKeepDataAll() {
        confirm("卸载（保留数据）", "已选中${apps.size}个应用，卸载后，这些应用的数据会被保留，这可能会导致下次安装不同签名的同名应用时无法安装，继续吗？", Runnable {
            _uninstallKeepDataAll()
        })
    }

    private fun _uninstallKeepDataAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[uninstall ${item.appName}]';")

            sb.append("pm uninstall -k $packageName;")
        }

        sb.append("echo '[operation completed]';")
        execShell(sb)
    }

    protected fun buildAll() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(context, "该功能只支持Android N（7.0）以上的系统！", Toast.LENGTH_SHORT).show()
            return
        }
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[compile ${item.appName}]'\n")

            sb.append("cmd package compile -m speed $packageName\n\n")
        }

        sb.append("echo '[operation completed]'\n\n")
        execShell(sb)
    }
}
