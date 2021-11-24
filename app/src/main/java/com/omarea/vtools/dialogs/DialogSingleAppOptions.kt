package com.omarea.vtools.dialogs

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.ui.DialogHelper
import com.omarea.model.AppInfo
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import java.io.File

/**
 * Created by Hello on 2018/01/26.
 */

class DialogSingleAppOptions(context: Activity, var app: AppInfo, handler: Handler) : DialogAppOptions(context, arrayListOf<AppInfo>(app), handler) {
    fun showSingleAppOptions() {
        when (app.appType) {
            AppInfo.AppType.USER -> showUserAppOptions()
            AppInfo.AppType.SYSTEM -> showSystemAppOptions()
            AppInfo.AppType.BACKUPFILE -> showBackupAppOptions()
            else -> {
                Toast.makeText(context, "UNSupport！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAppIcon(app: AppInfo): Drawable? {
        var icon: Drawable? = null
        try {
            val installInfo = context.packageManager.getPackageInfo(app.packageName.toString(), 0)
            icon = installInfo.applicationInfo.loadIcon(context.packageManager)
            return icon
        } catch (ex: Exception) {
        } finally {
        }
        return null
    }

    /**
     * 显示用户应用选项
     */
    private fun showUserAppOptions() {
        val dialogView = context.layoutInflater.inflate(R.layout.dialog_app_options_user, null)

        val dialog = DialogHelper.customDialog(context, dialogView)
        dialogView.findViewById<TextView>(R.id.app_target_sdk).text = "SDK" + app.targetSdkVersion.toString()
        dialogView.findViewById<TextView>(R.id.app_min_sdk).text = "SDK" + app.minSdkVersion.toString()
        dialogView.findViewById<TextView>(R.id.app_version_name).text = "Version Name: " + app.versionName
        dialogView.findViewById<TextView>(R.id.app_version_code).text = "Version Code: " + app.versionCode
        dialogView.findViewById<ImageView>(R.id.app_logo).setImageDrawable(loadAppIcon(app))

        dialogView.findViewById<View>(R.id.app_options_single_only).visibility = View.VISIBLE
        dialogView.findViewById<View>(R.id.app_options_copay_package).setOnClickListener {
            dialog.dismiss()
            copyPackageName()
        }
        dialogView.findViewById<TextView>(R.id.app_package_name).setText(app.packageName)
        dialogView.findViewById<View>(R.id.app_options_copay_path).setOnClickListener {
            dialog.dismiss()
            copyInstallPath()
        }
        dialogView.findViewById<TextView>(R.id.app_install_path).setText(app.path)
        dialogView.findViewById<View>(R.id.app_options_open_detail).setOnClickListener {
            dialog.dismiss()
            openDetails()
        }
        dialogView.findViewById<View>(R.id.app_options_app_store).setOnClickListener {
            dialog.dismiss()
            showInMarket()
        }
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
        dialogView.findViewById<View>(R.id.app_options_as_system).setOnClickListener {
            dialog.dismiss()
            moveToSystem()
        }
        dialogView.findViewById<View>(R.id.app_options_dex2oat).setOnClickListener {
            dialog.dismiss()
            buildAll()
        }
        dialogView.findViewById<TextView>(R.id.app_options_title).setText(app.appName)

        dialogView.findViewById<View>(R.id.app_options_app_freeze).setOnClickListener {
            dialog.dismiss()
            modifyStateAll()
        }
    }

    /**
     * 显示系统应用选项
     */
    private fun showSystemAppOptions() {
        val dialogView = context.layoutInflater.inflate(R.layout.dialog_app_options_system, null)

        val dialog = DialogHelper.customDialog(context, dialogView)
        dialogView.findViewById<TextView>(R.id.app_target_sdk).text = "SDK" + app.targetSdkVersion.toString()
        dialogView.findViewById<TextView>(R.id.app_min_sdk).text = "SDK" + app.minSdkVersion.toString()
        dialogView.findViewById<TextView>(R.id.app_version_name).text = "Version Name: " + app.versionName
        dialogView.findViewById<TextView>(R.id.app_version_code).text = "Version Code: " + app.versionCode
        dialogView.findViewById<ImageView>(R.id.app_logo).setImageDrawable(loadAppIcon(app))

        dialogView.findViewById<View>(R.id.app_options_single_only).visibility = View.VISIBLE
        dialogView.findViewById<View>(R.id.app_options_backup_apk).run {
            visibility = if (app.updated) View.VISIBLE else View.GONE

            if (app.updated) {
                setOnClickListener {
                    dialog.dismiss()

                    backupAll()
                }
            }
        }
        dialogView.findViewById<View>(R.id.app_options_copay_package).setOnClickListener {
            dialog.dismiss()
            copyPackageName()
        }
        dialogView.findViewById<TextView>(R.id.app_package_name).setText(app.packageName)
        dialogView.findViewById<View>(R.id.app_options_copay_path).setOnClickListener {
            dialog.dismiss()
            copyInstallPath()
        }
        dialogView.findViewById<TextView>(R.id.app_install_path).setText(app.path)
        dialogView.findViewById<View>(R.id.app_options_open_detail).setOnClickListener {
            dialog.dismiss()
            openDetails()
        }
        dialogView.findViewById<View>(R.id.app_options_app_store).setOnClickListener {
            dialog.dismiss()
            showInMarket()
        }
        dialogView.findViewById<View>(R.id.app_options_clear).setOnClickListener {
            dialog.dismiss()
            clearAll()
        }
        /*
        dialogView.findViewById<View>(R.id.app_options_backup_apk).setOnClickListener {
            dialog.dismiss()
            backupAll(true, false)
        }
        dialogView.findViewById<View>(R.id.app_options_backup_all).setOnClickListener {
            dialog.dismiss()
            backupAll(true, true)
        }
        */
        if (app.updated) {
            dialogView.findViewById<View>(R.id.app_options_uninstall_user).visibility = View.GONE
        } else {
            dialogView.findViewById<View>(R.id.app_options_uninstall_user).setOnClickListener {
                dialog.dismiss()
                uninstallAllSystem(app.updated)
            }
        }

        dialogView.findViewById<View>(R.id.app_options_dex2oat).setOnClickListener {
            dialog.dismiss()
            buildAll()
        }

        if (app.updated) {
            dialogView.findViewById<View>(R.id.app_options_delete).visibility = View.GONE
            dialogView.findViewById<View>(R.id.app_options_uninstall).setOnClickListener {
                dialog.dismiss()
                uninstallAllSystem(app.updated)
            }
        } else {
            dialogView.findViewById<View>(R.id.app_options_delete).setOnClickListener {
                dialog.dismiss()
                deleteAll()
            }
            dialogView.findViewById<View>(R.id.app_options_uninstall).visibility = View.GONE
        }

        dialogView.findViewById<TextView>(R.id.app_options_title).setText(app.appName)

        dialogView.findViewById<View>(R.id.app_options_app_freeze).setOnClickListener {
            dialog.dismiss()
            modifyStateAll()
        }
    }

    /**
     * 显示备份的应用选项
     */
    private fun showBackupAppOptions() {
        val view = context.layoutInflater.inflate(R.layout.dialog_app_restore, null)
        val dialog = DialogHelper.customDialog(context, view)

        view.findViewById<View>(R.id.app_install).run {
            setOnClickListener {
                restoreAll(apk = true)
            }
        }
        val dataExists = backupDataExists(app.packageName)
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
        view.findViewById<View>(R.id.app_copy_name).setOnClickListener {
            dialog.dismiss()
            copyPackageName()
        }
        view.findViewById<View>(R.id.app_go_store).setOnClickListener {
            dialog.dismiss()
            showInMarket()
        }
        view.findViewById<View>(R.id.app_delete_backup).setOnClickListener {
            dialog.dismiss()
            deleteBackupAll()
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
        cm.text = app.packageName
        Toast.makeText(context, "已复制：${app.packageName}", Toast.LENGTH_LONG).show()
    }

    private fun copyInstallPath() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.text = app.path
        Toast.makeText(context, "已复制：${app.path}", Toast.LENGTH_LONG).show()
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
            sb.append("busybox cp '${app.path}' '$outPutPath'\n")
            sb.append("chmod 0755 '$outPutPath'\n")
            sb.append("chown -R system:system '$outPutPath'\n")
            sb.append("busybox chown -R system:system '$outPutPath'\n")
            sb.append("if [[ ! -e '$outPutPath' ]]\n then exit 1\n else rm -f '${app.path}'\n fi\n\n")
        } else {
            val parent = File(appDir)
            val outPutPath = "/system/app/${parent.name}"
            sb.append("busybox cp -pdrf '$appDir' '/system/app/'\n")
            // sb.append("busybox cp -a '$appDir' '$outPutPath'\n")
            sb.append("chmod -R 0755 '$outPutPath'\n")
            sb.append("chown -R system:system '$outPutPath'\n")
            sb.append("busybox chown -R system:system '$outPutPath'\n")
            sb.append("if [[ ! -e '$outPutPath' ]]\n then exit 1\n exit 1\n else exit 0\n fi\n\n")
        }
        sb.append("sync\n")
        sb.append("sleep 1\n")
        sb.append("echo '[operation completed]'\n")
        execShell(sb)
    }

    private fun moveToSystemMagisk() {
        val appDir = File(app.path.toString()).parent
        val result = if (appDir == "/data/app") { // /data/app/xxx.apk
            val outPutPath = "/system/app/"
            MagiskExtend.createFileReplaceModule(outPutPath, app.path.toString(), app.packageName, app.appName)
        } else { // /data/app/xxx.xxx.xxx/xxx.apk
            val outPutPath = "/system/app/" + app.packageName
            MagiskExtend.createFileReplaceModule(outPutPath, appDir, app.packageName, app.appName)
        }
        if (result) {
            DialogHelper.helpInfo(context, "已通过Magisk完成操作，请重启手机~", "")
        } else {
            DialogHelper.helpInfo(context, "Magisk镜像空间不足，操作失败！~", "")
        }
    }

    private fun moveToSystem() {
        val magiskSupported = MagiskExtend.magiskSupported()
        if (!magiskSupported && isMagisk() && isTmpfs("/system/app")) {
            DialogHelper.helpInfo(context,
                    "Magisk 副作用警告",
                    "检测到你正在使用Magisk，并使用了一些会添加系统应用的模块，这导致/system/app被Magisk劫持并且无法写入！！"
            )
            return
        }
        val view = context.layoutInflater.inflate(R.layout.dialog_app_trans_mode, null)
        view.findViewById<TextView>(R.id.confirm_message).text = "部分应用迁移到系统目录会无法运行。\n\n此外，你需要解锁System分区，或安装Magisk(19.3+)。\n\n转换完成后，请重启手机！"
        val switchCreateModule = view.findViewById<CompoundButton>(R.id.trans_create_module)
        switchCreateModule.isEnabled = magiskSupported
        switchCreateModule.isChecked = magiskSupported

        val dialog = DialogHelper.customDialog(context, view)
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            if (switchCreateModule.isChecked && magiskSupported) {
                moveToSystemMagisk()
            } else {
                moveToSystemExec()
            }
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
    }
}
