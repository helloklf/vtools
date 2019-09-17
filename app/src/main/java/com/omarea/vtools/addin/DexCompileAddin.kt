package com.omarea.vtools.addin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.shell.AsynSuShellUnit
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.PropsUtils
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import com.omarea.vtools.services.CompileService
import java.util.*

/**
 * Created by Hello on 2018/02/20.
 */

class DexCompileAddin(private var context: Context) : AddinBase(context) {
    fun isSupport(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(context, "系统版本过低，至少需要Android 7.0！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    open class ProgressHandler(context: Context, total: Int) : Handler() {
        protected var dialog: View
        protected var alert: android.app.AlertDialog
        protected var textView: TextView
        protected var progressBar: ProgressBar
        private var total: Int = 0
        private var current: Int = 0

        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            try {
                if (msg.obj != null) {
                    if (msg.what == 0) {
                        textView.text = "正在执行操作..."
                    } else {
                        val obj = msg.obj.toString()
                        if (obj == "[operation completed]") {
                            progressBar.progress = 100
                            textView.text = "操作完成！"
                            this.postDelayed({
                                alert.dismiss()
                                alert.hide()
                            }, 2000)
                        } else if (Regex("^\\[.*]\$").matches(obj)) {
                            progressBar.progress = msg.what
                            val text = obj.replace("[compile ", "[编译 ").replace("[reset ", "[重置 ")
                            if (obj.contains("compile") || obj.contains("reset")) {
                                current++
                            }
                            textView.text = text + "\n(${current}/${total})"
                        }
                    }
                }
            } catch (ex: Exception) {
            }
        }

        init {
            val layoutInflater = LayoutInflater.from(context)
            this.total = total
            dialog = layoutInflater.inflate(R.layout.dialog_loading, null)
            alert = android.app.AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
            textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
            progressBar = (dialog.findViewById(R.id.dialog_app_details_progress) as ProgressBar)
            alert.window!!.setWindowAnimations(R.style.windowAnim)
            alert.show()
            textView.text = "正在获取权限"
        }
    }


    private fun getAllPackageNames(): ArrayList<String> {
        val packageManager: PackageManager = context.packageManager
        val packageInfos = packageManager.getInstalledApplications(0)
        val list = ArrayList<String>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            list.add(packageInfos[i].packageName)
        }

        return list
    }

    //增加进度显示，而且不再出现因为编译应用自身而退出
    private fun run2() {
        if (!isSupport()) {
            return
        }

        val arr = arrayOf("Speed编译(推荐)", "Speed编译(后台进行)", "Everything编译", "Everything编译(后台进行)", "重置(清除编译)")
        var index = 0
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle("请选择执行方式")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    val lastIndex = arr.size - 1
                    val apps = getAllPackageNames()
                    if (index == 1 || index == 3) {
                        if (CompileService.compiling) {
                            Toast.makeText(context, "有一个后台编译过程正在进行，不能重复开启", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                val service = Intent(context, CompileService::class.java)
                                service.action = context.getString(if (index == 1) R.string.scene_speed_compile else R.string.scene_everything_compile)
                                context.startService(service)
                                Toast.makeText(context, "开始后台编译，请查看通知了解进度", Toast.LENGTH_SHORT).show()
                            } catch (ex: java.lang.Exception) {
                                Toast.makeText(context, "启动后台过程失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        if (index == lastIndex) {
                            // miui com.miui.contentcatcher 停止会导致所有应用被关闭 - 屏蔽它
                            if (apps.contains("com.miui.contentcatcher")) {
                                apps.remove("com.miui.contentcatcher")
                            }
                            if (apps.contains("com.miui.catcherpatch")) {
                                apps.remove("com.miui.catcherpatch")
                            }
                            apps.remove(context.packageName)
                        }

                        val commands = StringBuilder()
                        val action = if (index == lastIndex) "reset" else "compile"
                        for (app in apps) {
                            commands.append("echo '[${action} ${app}]'")
                            commands.append(";\n`")
                            when (index) {
                                0 -> commands.append("cmd package compile -m speed ${app}")
                                2 -> commands.append("cmd package compile -m everything ${app}")
                                lastIndex -> commands.append("cmd package compile --reset ${app}")
                            }
                            commands.append("` > /dev/null;\n\n")
                        }
                        commands.append("echo '[operation completed]';")
                        commands.append("\n\n")
                        if (index == 0) {
                            commands.append("cmd package compile -m speed ${context.packageName}")
                        } else if (index == 2) {
                            commands.append("cmd package compile -m everything ${context.packageName}")
                        }
                        commands.append("\n\n")
                        AsynSuShellUnit(ProgressHandler(context, apps.size)).exec(commands.toString()).waitFor()
                    }
                })
                .setNeutralButton("查看说明", { _, _ ->
                    DialogHelper.animDialog(AlertDialog.Builder(context)
                            .setTitle("说明")
                            .setMessage(R.string.addin_dex2oat_helpinfo)
                            .setNegativeButton("了解更多", { dialog, which ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.addin_dex2oat_helplink))))
                            }))
                }))
    }

    override fun run() {
        run2()
    }

    fun modifyConfigOld() {
        val arr = arrayOf(
                "verify",
                "speed",
                "恢复默认")
        val intallMode = PropsUtils.getProp("dalvik.vm.dex2oat-filter")
        var index = 0
        when (intallMode) {
            "interpret-only" -> index = 0
            "speed" -> index = 1
        }
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle("请选择Dex2oat配置")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    val stringBuilder = StringBuilder()

                    //移除已添加的配置
                    stringBuilder.append("sed '/^dalvik.vm.image-dex2oat-filter=/'d /system/build.prop > /data/build.prop;")
                    stringBuilder.append("sed -i '/^dalvik.vm.dex2oat-filter=/'d /data/build.prop;")

                    when (index) {
                        0 -> {
                            stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=interpret-only' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=interpret-only' /data/build.prop;")
                        }
                        1 -> {
                            stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                        }
                    }

                    stringBuilder.append(CommonCmds.MountSystemRW)
                    stringBuilder.append("cp /system/build.prop /system/build.prop.${System.currentTimeMillis()}\n")
                    stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                    stringBuilder.append("rm /data/build.prop\n")
                    stringBuilder.append("chmod 0755 /system/build.prop\n")

                    execShell(stringBuilder)
                    Toast.makeText(context, "配置已修改，但需要重启才能生效！", Toast.LENGTH_SHORT).show()
                })
                .setNeutralButton("查看说明", { _, _ ->
                    AlertDialog.Builder(context).setTitle("说明").setMessage("interpret-only模式安装应用更快。speed模式安装应用将会很慢，但是运行速度更快。").create().show()
                }))
    }

    fun modifyConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(context, "系统版本过低，至少需要Android 7.0！", Toast.LENGTH_SHORT).show()
            modifyConfigOld()
            return
        }

        val arr = arrayOf(
                "不编译（优化安装速度）",
                "编译（优化运行速度）",
                "恢复默认")
        val intallMode = PropsUtils.getProp("pm.dexopt.install")
        var index = 0
        when (intallMode) {
            "extract",
            "quicken",
            "interpret-only",
            "verify-none" -> index = 0
            "speed" -> index = 1
            "everything" -> index = 1
            else -> {
                if (PropsUtils.getProp("pm.dexopt.core-app") == "verify-none") {
                    index = 3
                } else
                    index = 0
            }
        }
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle("请选择pm.dexopt策略")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    val stringBuilder = StringBuilder()

                    //移除已添加的配置
                    stringBuilder.append("cp /system/build.prop /data/build.prop;")
                    //stringBuilder.append("sed -i '/^pm.dexopt.ab-ota=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.bg-dexopt=/'d /data/build.prop;")
                    //stringBuilder.append("sed -i '/^pm.dexopt.boot=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.core-app=/'d /data/build.prop;")
                    //stringBuilder.append("sed -i '/^pm.dexopt.first-boot=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.forced-dexopt=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.install=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.nsys-library=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.shared-apk=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^dalvik.vm.image-dex2oat-filter=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^dalvik.vm.dex2oat-filter=/'d /data/build.prop;")

                    when (index) {
                        0 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=speed' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                stringBuilder.append("sed -i '\$apm.dexopt.install=interpret-only' /data/build.prop;")
                            } else {
                                stringBuilder.append("sed -i '\$apm.dexopt.install=quicken' /data/build.prop;")
                            }
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=speed' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                                stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                            }
                        }
                        1 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.install=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=speed' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                                stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                            }
                        }
                    }

                    stringBuilder.append(CommonCmds.MountSystemRW)
                    stringBuilder.append("cp /system/build.prop /system/build.prop.${System.currentTimeMillis()}\n")
                    stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                    stringBuilder.append("rm /data/build.prop\n")
                    stringBuilder.append("chmod 0755 /system/build.prop\n")

                    execShell(stringBuilder)
                    Toast.makeText(context, "配置已修改，但需要重启才能生效！", Toast.LENGTH_SHORT).show()
                })
                .setNeutralButton("查看说明", { _, _ ->
                    DialogHelper.animDialog(AlertDialog.Builder(context)
                            .setTitle("说明")
                            .setMessage(R.string.addin_dexopt_helpinfo)
                            .setNegativeButton("了解更多", { dialog, which ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.addin_dex2oat_helplink))))
                            }))
                }))
    }
}
