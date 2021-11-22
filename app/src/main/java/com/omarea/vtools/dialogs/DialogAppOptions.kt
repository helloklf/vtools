package com.omarea.vtools.dialogs

import android.app.Activity
import android.content.Context
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.shell.AsynSuShellUnit
import com.omarea.common.shell.KeepShell
import com.omarea.common.ui.DialogHelper
import com.omarea.model.AppInfo
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import java.io.File
import java.util.*

/**
 * Created by helloklf on 2017/12/04.
 */

open class DialogAppOptions(protected final var context: Activity, protected var apps: ArrayList<AppInfo>, protected var handler: Handler) {
    private var allowPigz = false
    private var backupPath = CommonCmds.AbsBackUpDir
    private var userdataPath = ""

    init {
        userdataPath = context.filesDir.absolutePath
        userdataPath = userdataPath.substring(0, userdataPath.indexOf(context.packageName) - 1)
    }

    fun selectUserAppOptions() {
        val dialogView = context.layoutInflater.inflate(R.layout.dialog_app_options_user, null)

        val dialog = DialogHelper.customDialog(context, dialogView)
        dialogView.findViewById<View>(R.id.app_options_single_only).visibility = View.GONE
        dialogView.findViewById<View>(R.id.app_options_clear).setOnClickListener {
            dialog.dismiss()
            clearAll()
        }
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            dialogView.findViewById<View>(R.id.app_options_backup_apk).visibility = View.GONE
        } else {
            dialogView.findViewById<View>(R.id.app_options_backup_apk).setOnClickListener {
                dialog.dismiss()
                backupAll()
            }
        }
        dialogView.findViewById<View>(R.id.app_options_uninstall).setOnClickListener {
            dialog.dismiss()
            uninstallAll()
        }
        /*
        dialogView.findViewById<View>(R.id.app_options_as_system).setOnClickListener {
            dialog.dismiss()
            moveToSystem()
        }
        */
        dialogView.findViewById<View>(R.id.app_options_dex2oat).setOnClickListener {
            dialog.dismiss()
            buildAll()
        }
        dialogView.findViewById<TextView>(R.id.app_options_title).text = "请选择操作"

        dialogView.findViewById<View>(R.id.app_options_app_freeze).setOnClickListener {
            dialog.dismiss()
            modifyStateAll()
        }
    }

    fun selectSystemAppOptions() {
        val dialogView = context.layoutInflater.inflate(R.layout.dialog_app_options_system, null)

        val dialog = DialogHelper.customDialog(context, dialogView)
        dialogView.findViewById<View>(R.id.app_options_single_only).visibility = View.GONE
        dialogView.findViewById<View>(R.id.app_options_clear).setOnClickListener {
            dialog.dismiss()
            clearAll()
        }
        dialogView.findViewById<View>(R.id.app_options_uninstall_user).setOnClickListener {
            dialog.dismiss()
            uninstallAllSystem(false) // TODO:xxx
        }
        dialogView.findViewById<View>(R.id.app_options_dex2oat).setOnClickListener {
            dialog.dismiss()
            buildAll()
        }

        dialogView.findViewById<View>(R.id.app_options_delete).setOnClickListener {
            dialog.dismiss()
            deleteAll()
        }
        dialogView.findViewById<View>(R.id.app_options_uninstall).visibility = View.GONE

        dialogView.findViewById<TextView>(R.id.app_options_title).setText("请选择操作")

        dialogView.findViewById<View>(R.id.app_options_app_freeze).setOnClickListener {
            dialog.dismiss()
            modifyStateAll()
        }
    }

    fun selectBackupOptions() {
        val view = context.layoutInflater.inflate(R.layout.dialog_app_restore, null)
        val dialog = DialogHelper.customDialog(context, view)
        view.findViewById<View>(R.id.app_install).run {
            setOnClickListener {
                dialog.dismiss()
                restoreAll(apk = true)
            }
        }

        val dataExists = (apps.find {
            backupDataExists(it.packageName)
        }) != null

        view.findViewById<View>(R.id.app_restore_full).run {
            visibility = if (dataExists) View.VISIBLE else View.GONE
            setOnClickListener {
                dialog.dismiss()
                restoreAll(apk = true)
            }
        }
        view.findViewById<View>(R.id.app_restore_data).run {
            visibility = if (dataExists) View.VISIBLE else View.GONE
            setOnClickListener {
                dialog.dismiss()
                restoreAll(apk = false)
            }
        }
        view.findViewById<View>(R.id.app_delete_backup).setOnClickListener {
            dialog.dismiss()
            deleteBackupAll()
        }

    }

    protected fun isMagisk(): Boolean {
        val keepShell = KeepShell(false)
        val result = keepShell.doCmdSync("su -v").toUpperCase(Locale.getDefault()).contains("MAGISKSU")
        keepShell.tryExit()
        return result
    }

    protected fun isTmpfs(dir: String): Boolean {
        val keepShell = KeepShell(false)
        val result = keepShell.doCmdSync("df | grep tmpfs | grep \"$dir\"").toUpperCase(Locale.getDefault()).trim().isNotEmpty()
        keepShell.tryExit()
        return result
    }

    protected fun execShell(sb: StringBuilder) {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_loading, null)
        val textView = (dialog.findViewById(R.id.dialog_text) as TextView)
        textView.text = "正在获取权限"
        val alert = DialogHelper.customDialog(context, dialog, false)
        AsynSuShellUnit(ProgressHandler(dialog, alert, handler)).exec(sb.toString()).waitFor()
    }

    open class ProgressHandler(dialog: View, private var alert: DialogHelper.DialogWrap, protected var handler: Handler) : Handler(Looper.getMainLooper()) {
        private var textView: TextView = (dialog.findViewById(R.id.dialog_text) as TextView)
        private var progressBar: ProgressBar = (dialog.findViewById(R.id.dialog_app_details_progress) as ProgressBar)
        private var error = java.lang.StringBuilder()

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.obj != null) {
                if (msg.what == 0) {
                    textView.text = "正在执行操作..."
                } else if (msg.what == 5) {
                    error.append(msg.obj)
                    error.append("\n")
                } else if (msg.what == 10) {
                    if (msg.obj == true) {
                        textView.text = "操作完成！"
                    } else {
                        textView.text = "出现异常！"
                    }
                    handler.postDelayed({
                        alert.dismiss()
                        alert.hide()
                    }, 2000)
                } else {
                    val obj = msg.obj.toString()
                    if (obj.contains("[operation completed]")) {
                        progressBar.progress = 100
                        textView.text = "操作完成！"
                        handler.postDelayed({
                            try {
                                alert.dismiss()
                                alert.hide()
                            } catch (ex: Exception) {
                            }
                            if (error.isNotBlank()) {
                                val context: Context = alert.context
                                DialogHelper.alert(context, "出现了一些错误", error.toString())
                            }
                        }, 1200)
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
        DialogHelper.confirmBlur(context, title, msg, next)
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
    protected fun backupAll() {
        val view = context.layoutInflater.inflate(R.layout.dialog_app_backup_mode, null)
        view.findViewById<TextView>(R.id.confirm_message).text = "备份选中的 ${apps.size} 个应用和数据？"

        val dialog = DialogHelper.customDialog(context, view)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            _backupAll()
        }
    }

    private fun _backupAll() {
        checkPigz()

        val date = Date().time.toString()

        val sb = StringBuilder()
        sb.append("backup_date=\"$date\"\n")
        sb.append("\n")
        sb.append("backup_path=\"${CommonCmds.AbsBackUpDir}\"\n")
        sb.append("mkdir -p \${backup_path}\n")
        sb.append("\n")
        sb.append("\n")

        for (item in apps) {
            val packageName = item.packageName.toString()
            val path = item.path.toString()

            sb.append("rm -f \${backup_path}$packageName.apk\n")
            sb.append("\n")
            sb.append("echo '[copy $packageName.apk]'\n")
            sb.append("busybox cp -f $path \${backup_path}$packageName.apk\n")
            sb.append("\n")
        }
        sb.append("cd \${backup_path}\n")
        sb.append("chown sdcard_rw:sdcard_rw *\n")
        sb.append("chmod 777 *\n")

        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    /**
     * 还原选中的应用
     */
    protected fun restoreAll(apk: Boolean = true) {
        confirm("还原应用", "还原选中的 ${apps.size} 个应用和数据？") {
            _restoreAll(apk)
        }
    }

    protected fun backupDataExists(packageName: String): Boolean {
        return File("$backupPath$packageName.tar.gz").exists()
    }

    private fun _restoreAll(apk: Boolean = true) {
        val installApkTemp = FileWrite.getPrivateFilePath(context, "app_install_cache.apk")
        checkPigz()

        val sb = StringBuilder()
        sb.append("chown -R sdcard_rw:sdcard_rw \"$backupPath\" 2>/dev/null\n")
        sb.append("chmod -R 777 \"$backupPath\" 2>/dev/null\n")
        for (item in apps) {
            val packageName = item.packageName
            val apkPath = item.path.toString()
            if (apk && File(apkPath).exists()) {
                sb.append("echo '[install ${item.appName}]'\n")
                sb.append("rm -f $installApkTemp\n")
                sb.append("cp \"$apkPath\" $installApkTemp\n")
                sb.append("pm install -r $installApkTemp 1> /dev/null\n")
                sb.append("rm -f $installApkTemp\n")
            } else if (apk && File("$backupPath$packageName.apk").exists()) {
                sb.append("echo '[install ${item.appName}]'\n")
                sb.append("rm -f $installApkTemp\n")
                sb.append("cp \"$backupPath$packageName.apk\" $installApkTemp\n")
                sb.append("pm install -r $installApkTemp 1> /dev/null\n")
                sb.append("rm -f $installApkTemp\n")
            }
        }
        sb.append("sync\n")
        sb.append("sleep 2\n")
        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    /**
     * 禁用所选的应用
     */
    protected fun modifyStateAll() {
        val view = context.layoutInflater.inflate(R.layout.dialog_app_disable_mode, null)
        val dialog = DialogHelper.customDialog(context, view)
        view.findViewById<TextView>(R.id.confirm_message).text = "选中了 ${apps.size} 个应用，你希望把它们的状态改成？"

        val switchSuspend = view.findViewById<CompoundButton>(R.id.disable_suspend)
        val switchFreeze = view.findViewById<CompoundButton>(R.id.disable_freeze)
        val switchHide = view.findViewById<CompoundButton>(R.id.disable_hide)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            switchSuspend.isEnabled = false
            switchSuspend.isEnabled = true
        }
        switchSuspend.isChecked = apps.filter { it.suspended }.size == apps.size
        switchFreeze.isChecked = apps.filter { !it.enabled }.size == apps.size

        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            val suspend = switchSuspend.isChecked
            val freeze = switchFreeze.isChecked
            val hide = switchHide.isChecked
            _modifyStateAll(suspend, freeze, hide)
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun _modifyStateAll(suspend: Boolean, freeze: Boolean, hide: Boolean) {
        val androidP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName
            if (suspend) {
                if (!item.suspended) {
                    sb.append("echo '[suspend ${item.appName}]'\n")
                    sb.append("pm suspend $packageName\n")
                }
            } else if (androidP) {
                if (item.suspended) {
                    sb.append("echo '[unsuspend ${item.appName}]'\n")
                    sb.append("am kill $packageName 2>/dev/null\n")
                    sb.append("pm unsuspend $packageName\n")
                }
            }

            if (freeze) {
                if (item.enabled) {
                    sb.append("echo '[disable ${item.appName}]'\n")
                    sb.append("pm disable ${packageName}\n")
                }
            } else {
                if (!item.enabled) {
                    sb.append("echo '[enable ${item.appName}]'\n")
                    sb.append("pm enable ${packageName}\n")
                }
            }

            if (hide) {
                sb.append("echo '[hide ${item.appName}]'\n")
                sb.append("pm hide ${packageName}\n")
            }

        }

        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    /**
     * 删除选中的应用
     */
    protected fun deleteAll() {
        confirm("删除应用", "已选择 ${apps.size} 个应用，删除系统应用可能导致功能不正常，甚至无法开机，确定要继续删除？") {
            if (isMagisk() && !MagiskExtend.moduleInstalled() && (isTmpfs("/system/app") || isTmpfs("/system/priv-app"))) {
                DialogHelper.confirm(context,
                        "Magisk 副作用警告",
                        "检测到你正在使用Magisk作为ROOT权限管理器，并且/system/app和/system/priv-app目录已被某些模块修改，这可能导致这些目录被Magisk劫持并且无法写入！！",
                        DialogHelper.DialogButton(context.getString(R.string.btn_continue), {
                            _deleteAll()
                        }))
            } else {
                _deleteAll()
            }
        }
    }

    private fun _deleteAll() {
        val sb = StringBuilder()
        sb.append(CommonCmds.MountSystemRW)
        var useMagisk = false
        for (item in apps) {
            val packageName = item.packageName
            // 先禁用再删除，避免老弹停止运行
            sb.append("echo '[disable ${item.appName}]'\n")
            sb.append("pm disable $packageName\n")

            sb.append("echo '[delete ${item.appName}]'\n")
            if (MagiskExtend.moduleInstalled()) {
                MagiskExtend.deleteSystemPath(item.path.toString())
                useMagisk = true
            } else {
                val dir = item.dir.toString()

                sb.append("rm -rf $dir/oat\n")
                sb.append("rm -rf $dir/lib\n")
                sb.append("rm -rf '${item.path}'\n")
            }
        }

        sb.append("echo '[operation completed]'\n")
        execShell(sb)
        if (useMagisk) {
            DialogHelper.helpInfo(context, "已通过Magisk完成操作，请重启手机~", "")
        }
    }

    /**
     * 删除备份
     */
    protected fun deleteBackupAll() {
        confirm("删除备份", "永久删除这些备份文件？") {
            _deleteBackupAll()
        }
    }

    private fun _deleteBackupAll() {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName
            sb.append("echo '[delete ${item.appName}]'\n")

            if (item.path != null) {
                sb.append("rm -rf '${item.path}'\n")
                if (item.path == "$backupPath$packageName.apk") {
                    sb.append("rm -rf $backupPath$packageName.tar.gz\n")
                }
            } else {
                sb.append("rm -rf $backupPath$packageName.apk\n")
                sb.append("rm -rf $backupPath$packageName.tar.gz\n")
            }
        }

        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    /**
     * 清除数据
     */
    protected fun clearAll() {
        val view = context.layoutInflater.inflate(R.layout.dialog_app_clear_mode, null)
        view.findViewById<TextView>(R.id.confirm_message).text = "确定将选中的 ${apps.size} 个应用数据清空？"

        val dialog = DialogHelper.customDialog(context, view)
        val userOnly = view.findViewById<CompoundButton>(R.id.clear_user_only)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            _clearAll(userOnly.isChecked)
        }
    }

    private fun _clearAll(userOnly: Boolean) {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager?
        val userHandle = android.os.Process.myUserHandle()
        var uid = 0L
        if (um != null) {
            uid = um.getSerialNumberForUser(userHandle)
        } else {
            Toast.makeText(context, "获取用户ID失败！", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[clear ${item.appName}]'\n")

            if (userOnly) {
                sb.append("pm clear --user $uid $packageName\n")
            } else {
                sb.append("pm clear $packageName\n")
            }
        }

        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    /**
     * 卸载选中
     */
    protected fun uninstallAll() {
        val view = context.layoutInflater.inflate(R.layout.dialog_app_uninstall_mode, null)
        view.findViewById<TextView>(R.id.confirm_message).text = "确定卸载选中的 ${apps.size} 个应用？"

        val dialog = DialogHelper.customDialog(context, view)
        val userOnly = view.findViewById<CompoundButton>(R.id.uninstall_user_only)
        val keepData = view.findViewById<CompoundButton>(R.id.uninstall_keep_data)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            _uninstallAll(userOnly.isChecked, keepData.isChecked)
        }
    }

    /**
     * 卸载选中
     */
    protected fun uninstallAllSystem(updated: Boolean) {
        val view = context.layoutInflater.inflate(R.layout.dialog_app_uninstall_mode, null)
        view.findViewById<TextView>(R.id.confirm_message).text = "确定卸载选中的 ${apps.size} 个系统应用？"

        val dialog = DialogHelper.customDialog(context, view)
        val userOnly = view.findViewById<CompoundButton>(R.id.uninstall_user_only)
        val keepData = view.findViewById<CompoundButton>(R.id.uninstall_keep_data)

        userOnly.isEnabled = false
        if (updated) {
            userOnly.isEnabled = false
            keepData.isEnabled = false

            userOnly.isChecked = false
            keepData.isChecked = false
        } else {
            userOnly.isEnabled = false
            userOnly.isChecked = true
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            _uninstallAll(userOnly.isChecked, keepData.isChecked)
        }
    }

    private fun _uninstallAll(userOnly: Boolean, keepData: Boolean) {
        if (userOnly) {
            val um = context.getSystemService(Context.USER_SERVICE) as UserManager?
            val userHandle = android.os.Process.myUserHandle()
            if (um != null) {
                val uid = um.getSerialNumberForUser(userHandle)
                _uninstallAllOnlyUser(uid, keepData)
            } else {
                Toast.makeText(context, "获取用户ID失败！", Toast.LENGTH_SHORT).show()
            }
        } else {
            val sb = StringBuilder()

            for (item in apps) {
                val packageName = item.packageName
                sb.append("echo '[uninstall ${item.appName}]'\n")

                if (keepData) {
                    sb.append("pm uninstall -k $packageName\n")
                } else {
                    sb.append("pm uninstall $packageName\n")
                }
            }

            sb.append("echo '[operation completed]'\n")
            execShell(sb)
        }
    }

    private fun _uninstallAllOnlyUser(uid: Long, keepData: Boolean) {
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName
            sb.append("echo '[uninstall ${item.appName}]'\n")

            if (keepData) {
                sb.append("pm uninstall -k --user $uid $packageName\n")
            } else {
                sb.append("pm uninstall --user $uid $packageName\n")
            }
        }

        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    protected fun buildAll() {
        val view = context.layoutInflater.inflate(R.layout.dialog_app_dex2oat_mode, null)
        view.findViewById<TextView>(R.id.confirm_message).text = "dex2oat编译可提升(低端机)运行应用时的响应速度，但会显著增加存储空间占用。\n\n确定为选中的 ${apps.size} 个应用进行dex2oat编译吗？"
        val switchEverything = view.findViewById<CompoundButton>(R.id.dex2oat_everything)
        val switchForce = view.findViewById<CompoundButton>(R.id.dex2oat_force)

        val dialog = DialogHelper.customDialog(context, view)
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            if (switchEverything.isChecked) {
                buildAll("everything", switchForce.isChecked)
            } else {
                buildAll("speed", switchForce.isChecked)
            }
        }

    }

    private fun buildAll(mode: String, forced: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(context, "该功能只支持Android N（7.0）以上的系统！", Toast.LENGTH_SHORT).show()
            return
        }
        val sb = StringBuilder()
        for (item in apps) {
            val packageName = item.packageName.toString()
            sb.append("echo '[compile ${item.appName}]'\n")

            if (forced) {
                sb.append("cmd package compile -f -m $mode $packageName\n\n")
            } else {
                sb.append("cmd package compile -m $mode $packageName\n\n")
            }
        }

        sb.append("echo '[operation completed]'\n\n")
        execShell(sb)
    }
}
